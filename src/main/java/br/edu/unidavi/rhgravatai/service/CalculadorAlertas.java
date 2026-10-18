package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.config.PropriedadesAlertas;
import br.edu.unidavi.rhgravatai.model.Carga;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.CondicoesVinculo;
import br.edu.unidavi.rhgravatai.service.ResolvedorTeto.TetoResolvido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Recalcula os alertas a cada carga publicada, dentro da mesma transação (RF06, FR-037 a
 * FR-041). Os alertas são sinalizações para verificação, não afirmação de irregularidade.
 */
@Component
public class CalculadorAlertas implements AposPublicacao {

    private static final Logger log = LoggerFactory.getLogger(CalculadorAlertas.class);

    private final JdbcClient jdbc;
    private final CargaRepository cargas;
    private final ResolvedorTeto resolvedorTeto;
    private final PropriedadesAlertas propriedades;

    public CalculadorAlertas(JdbcClient jdbc, CargaRepository cargas, ResolvedorTeto resolvedorTeto,
                             PropriedadesAlertas propriedades) {
        this.jdbc = jdbc;
        this.cargas = cargas;
        this.resolvedorTeto = resolvedorTeto;
        this.propriedades = propriedades;
    }

    @Override
    public void aposPublicar(long cargaId) {
        Carga carga = cargas.buscar(cargaId).orElseThrow();
        jdbc.sql("DELETE FROM alerta").update();

        TetoResolvido teto = resolvedorTeto.resolver(carga.dataInicio());
        cargas.registrarTeto(cargaId, teto.valor(), teto.origem());
        int acimaDoTeto = teto.valor() == null ? 0 : sinalizarAcimaDoTeto(cargaId, teto);
        int foraDoPadrao = sinalizarForaDoPadrao(cargaId);

        log.info("Carga {}: teto {} ({}); {} alerta(s) de teto e {} fora do padrão",
                cargaId, teto.valor(), teto.origem(), acimaDoTeto, foraDoPadrao);
    }

    /** Salário base maior que o teto; igual não é sinalizado; o prefeito nunca é (FR-037). */
    private int sinalizarAcimaDoTeto(long cargaId, TetoResolvido teto) {
        return jdbc.sql("""
                        INSERT INTO alerta (carga_id, vinculo_id, regra, valor_vinculo, valor_referencia,
                                            diferenca, diferenca_percentual)
                        SELECT :carga, id, 'TETO', salario_base, :teto, salario_base - :teto,
                               round((salario_base - :teto) * 100 / :teto, 2)
                          FROM vinculo
                         WHERE %s AND salario_base > :teto AND id <> :prefeito""".formatted(CondicoesVinculo.ATIVO))
                .param("carga", cargaId)
                .param("teto", teto.valor())
                .param("prefeito", teto.prefeitoId() == null ? -1L : teto.prefeitoId())
                .update();
    }

    /**
     * Salário base que difere mais que o percentual configurado da mediana do grupo de vínculos
     * ativos com o mesmo cargo, nível e horas mensais. Grupos menores que o mínimo, nível
     * ausente e salário zero ficam de fora (FR-038, FR-039).
     */
    private int sinalizarForaDoPadrao(long cargaId) {
        String ativo = CondicoesVinculo.ativo("v.");
        return jdbc.sql("""
                        WITH grupos AS (
                            SELECT v.cargo, v.nivel, v.horas_mensais_min,
                                   CAST(percentile_cont(0.5) WITHIN GROUP (ORDER BY v.salario_base) AS numeric) AS mediana,
                                   count(*) AS tamanho
                              FROM vinculo v
                             WHERE %1$s AND v.nivel IS NOT NULL AND v.salario_base > 0
                             GROUP BY v.cargo, v.nivel, v.horas_mensais_min
                            HAVING count(*) >= :minimo
                        )
                        INSERT INTO alerta (carga_id, vinculo_id, regra, valor_vinculo, valor_referencia,
                                            diferenca, diferenca_percentual, grupo_tamanho)
                        SELECT :carga, v.id, 'FORA_PADRAO', v.salario_base, round(g.mediana, 2),
                               v.salario_base - round(g.mediana, 2),
                               round((v.salario_base - g.mediana) * 100 / g.mediana, 2), g.tamanho
                          FROM vinculo v
                          JOIN grupos g ON v.cargo IS NOT DISTINCT FROM g.cargo
                                       AND v.nivel = g.nivel
                                       AND v.horas_mensais_min IS NOT DISTINCT FROM g.horas_mensais_min
                         WHERE %1$s AND v.nivel IS NOT NULL AND v.salario_base > 0
                           AND abs(v.salario_base - g.mediana) * 100 > g.mediana * :percentual""".formatted(ativo))
                .param("carga", cargaId)
                .param("minimo", propriedades.foraPadrao().minimoGrupo())
                .param("percentual", propriedades.foraPadrao().percentual())
                .update();
    }
}
