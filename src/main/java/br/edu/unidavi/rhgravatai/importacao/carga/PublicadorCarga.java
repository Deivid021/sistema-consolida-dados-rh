package br.edu.unidavi.rhgravatai.importacao.carga;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica uma carga conciliada: tudo numa transação, para o site nunca ver dados pela metade
 * (FR-003, FR-008, FR-010, FR-011, FR-012).
 */
@Component
public class PublicadorCarga {

    /** Colunas copiadas da área de preparação para vinculo. */
    private static final List<String> COLUNAS = List.of(
            "entidade", "matricula", "nome", "situacao", "data_admissao", "data_desligamento",
            "motivo_desligamento", "regime", "classificacao", "forma_investidura", "secretaria",
            "local_trabalho", "cargo", "funcao", "nivel", "salario_base", "horario",
            "horas_mensais_min", "horas_semanais_min", "valores_originais", "texto_busca");

    /** Atributos cujas mudanças entram no histórico (FR-010). */
    private static final List<String> ATRIBUTOS_HISTORICO = List.of(
            "situacao", "regime", "secretaria", "local_trabalho", "cargo", "funcao", "nivel",
            "salario_base", "horas_mensais_min", "horas_semanais_min", "data_desligamento");

    private final JdbcClient jdbc;
    private final CargaRepository cargas;
    private final ObjectProvider<AposPublicacao> etapasPosteriores;

    public PublicadorCarga(JdbcClient jdbc, CargaRepository cargas, ObjectProvider<AposPublicacao> etapasPosteriores) {
        this.jdbc = jdbc;
        this.cargas = cargas;
        this.etapasPosteriores = etapasPosteriores;
    }

    @Transactional
    public void publicar(long cargaId) {
        boolean primeiraCarga = jdbc.sql("SELECT NOT EXISTS (SELECT 1 FROM vinculo)").query(Boolean.class).single();

        if (!primeiraCarga) {
            registrarMudancas(cargaId);
        }
        int alterados = jdbc.sql("SELECT count(DISTINCT vinculo_id) FROM mudanca_vinculo WHERE carga_id = :carga")
                .param("carga", cargaId).query(Integer.class).single();
        int novos = jdbc.sql("""
                        SELECT count(*) FROM vinculo_staging s
                         WHERE s.carga_id = :carga
                           AND NOT EXISTS (SELECT 1 FROM vinculo v
                                            WHERE v.entidade = s.entidade AND v.matricula = s.matricula)""")
                .param("carga", cargaId).query(Integer.class).single();

        atualizarVinculos(cargaId);
        marcarAusentes(cargaId);

        int naoEncontrados = jdbc.sql("SELECT count(*) FROM vinculo WHERE NOT ativo_na_fonte")
                .query(Integer.class).single();

        etapasPosteriores.orderedStream().forEach(etapa -> etapa.aposPublicar(cargaId));

        jdbc.sql("DELETE FROM vinculo_staging WHERE carga_id = :carga").param("carga", cargaId).update();
        cargas.concluirComSucesso(cargaId, novos, alterados, naoEncontrados);
    }

    private void registrarMudancas(long cargaId) {
        for (String atributo : ATRIBUTOS_HISTORICO) {
            jdbc.sql("""
                            INSERT INTO mudanca_vinculo (vinculo_id, carga_id, atributo, valor_anterior, valor_novo)
                            SELECT v.id, :carga, '%1$s', v.%1$s::text, s.%1$s::text
                              FROM vinculo v
                              JOIN vinculo_staging s ON s.entidade = v.entidade AND s.matricula = v.matricula
                             WHERE s.carga_id = :carga AND v.%1$s IS DISTINCT FROM s.%1$s""".formatted(atributo))
                    .param("carga", cargaId)
                    .update();
        }
        // Vínculos que voltaram a aparecer na fonte
        jdbc.sql("""
                        INSERT INTO mudanca_vinculo (vinculo_id, carga_id, atributo, valor_anterior, valor_novo)
                        SELECT v.id, :carga, 'ativo_na_fonte', 'false', 'true'
                          FROM vinculo v
                          JOIN vinculo_staging s ON s.entidade = v.entidade AND s.matricula = v.matricula
                         WHERE s.carga_id = :carga AND NOT v.ativo_na_fonte""")
                .param("carga", cargaId)
                .update();
    }

    private void atualizarVinculos(long cargaId) {
        String colunas = String.join(", ", COLUNAS);
        String atualizacoes = COLUNAS.stream()
                .filter(c -> !c.equals("entidade") && !c.equals("matricula"))
                .map(c -> c + " = EXCLUDED." + c)
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();
        jdbc.sql("""
                        INSERT INTO vinculo (%s, ativo_na_fonte, primeira_carga_id, ultima_carga_id, visto_por_ultimo_em)
                        SELECT %s, true, :carga, :carga, now()
                          FROM vinculo_staging
                         WHERE carga_id = :carga
                        ON CONFLICT (entidade, matricula) DO UPDATE SET
                            %s,
                            ativo_na_fonte = true,
                            ultima_carga_id = EXCLUDED.ultima_carga_id,
                            visto_por_ultimo_em = EXCLUDED.visto_por_ultimo_em""".formatted(colunas, colunas, atualizacoes))
                .param("carga", cargaId)
                .update();
    }

    /** Quem não veio nesta carga é mantido e marcado como não encontrado; nunca é apagado. */
    private void marcarAusentes(long cargaId) {
        jdbc.sql("""
                        INSERT INTO mudanca_vinculo (vinculo_id, carga_id, atributo, valor_anterior, valor_novo)
                        SELECT id, :carga, 'ativo_na_fonte', 'true', 'false'
                          FROM vinculo
                         WHERE ativo_na_fonte AND ultima_carga_id <> :carga""")
                .param("carga", cargaId)
                .update();
        jdbc.sql("UPDATE vinculo SET ativo_na_fonte = false WHERE ativo_na_fonte AND ultima_carga_id <> :carga")
                .param("carga", cargaId)
                .update();
    }
}
