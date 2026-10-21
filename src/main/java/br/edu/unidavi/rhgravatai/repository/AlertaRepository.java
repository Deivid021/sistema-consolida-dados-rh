package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.model.Alerta;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tabela alerta: cálculo das regras (FR-037 a FR-039) e listagem (FR-040 a FR-042). */
@Repository
public class AlertaRepository {

    private static final String ORDEM = "abs(a.diferenca_percentual) DESC, v.nome NULLS LAST, v.id";

    private final JdbcClient jdbc;

    public AlertaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void apagarTodos() {
        jdbc.sql("DELETE FROM alerta").update();
    }

    /** Salário base maior que o teto; igual não é sinalizado; o prefeito nunca é. */
    public int inserirAcimaDoTeto(long cargaId, BigDecimal teto, Long prefeitoId) {
        return jdbc.sql("""
                        INSERT INTO alerta (carga_id, vinculo_id, regra, valor_vinculo, valor_referencia,
                                            diferenca, diferenca_percentual)
                        SELECT :carga, id, 'TETO', salario_base, :teto, salario_base - :teto,
                               round((salario_base - :teto) * 100 / :teto, 2)
                          FROM vinculo
                         WHERE %s AND salario_base > :teto AND id <> :prefeito""".formatted(FiltroSql.ativo("")))
                .param("carga", cargaId)
                .param("teto", teto)
                .param("prefeito", prefeitoId == null ? -1L : prefeitoId)
                .update();
    }

    /**
     * Salário base que difere mais que {@code percentual} da mediana do grupo de vínculos ativos
     * com o mesmo cargo, nível e horas mensais. Grupos menores que {@code minimoGrupo}, nível
     * ausente e salário zero ficam de fora.
     */
    public int inserirForaDoPadrao(long cargaId, BigDecimal percentual, int minimoGrupo) {
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
                           AND abs(v.salario_base - g.mediana) * 100 > g.mediana * :percentual""".formatted(FiltroSql.ativo("v.")))
                .param("carga", cargaId)
                .param("minimo", minimoGrupo)
                .param("percentual", percentual)
                .update();
    }

    public List<Alerta> buscar(RegraAlerta regra, FiltroVinculos filtro, Paginacao paginacao) {
        FiltroSql sql = filtro(regra, filtro);
        return jdbc.sql(select(sql) + " ORDER BY " + ORDEM + " LIMIT :limite OFFSET :deslocamento")
                .params(sql.com(Map.of("limite", paginacao.tamanho(), "deslocamento", paginacao.deslocamento())))
                .query(AlertaRepository::mapear)
                .list();
    }

    public long contar(RegraAlerta regra, FiltroVinculos filtro) {
        FiltroSql sql = filtro(regra, filtro);
        return jdbc.sql("SELECT count(*) FROM alerta a JOIN vinculo v ON v.id = a.vinculo_id WHERE " + sql.where())
                .params(sql.parametros()).query(Long.class).single();
    }

    /** Todos os alertas do filtro, para a exportação. Fechar o fluxo depois do uso. */
    public Stream<Alerta> fluxo(RegraAlerta regra, FiltroVinculos filtro) {
        FiltroSql sql = filtro(regra, filtro);
        return jdbc.sql(select(sql) + " ORDER BY " + ORDEM)
                .params(sql.parametros())
                .withFetchSize(1000)
                .query(AlertaRepository::mapear)
                .stream();
    }

    private static FiltroSql filtro(RegraAlerta regra, FiltroVinculos filtro) {
        FiltroSql sql = FiltroSql.de(filtro, "v.");
        if (regra == null) {
            return sql;
        }
        Map<String, Object> parametros = new HashMap<>(sql.parametros());
        parametros.put("regra", regra.name());
        return new FiltroSql(sql.where() + " AND a.regra = :regra", parametros);
    }

    private static String select(FiltroSql sql) {
        return "SELECT a.regra, a.valor_vinculo, a.valor_referencia, a.diferenca, a.diferenca_percentual,"
                + " a.grupo_tamanho, " + VinculoRepository.colunas("v.")
                + " FROM alerta a JOIN vinculo v ON v.id = a.vinculo_id WHERE " + sql.where();
    }

    private static Alerta mapear(ResultSet rs, int linha) throws SQLException {
        return new Alerta(
                RegraAlerta.valueOf(rs.getString("regra")),
                VinculoRepository.mapear(rs, linha),
                rs.getBigDecimal("valor_vinculo"),
                rs.getBigDecimal("valor_referencia"),
                rs.getBigDecimal("diferenca"),
                rs.getBigDecimal("diferenca_percentual"),
                (Integer) rs.getObject("grupo_tamanho"));
    }
}
