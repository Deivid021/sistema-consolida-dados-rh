package br.edu.unidavi.rhgravatai.repository;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Histórico de mudanças detectadas entre cargas (FR-010). */
@Repository
public class MudancaVinculoRepository {

    /** Atributos cujas mudanças entram no histórico; vêm só desta lista, nunca do usuário. */
    private static final List<String> ATRIBUTOS = List.of(
            "situacao", "regime", "secretaria", "local_trabalho", "cargo", "funcao", "nivel",
            "salario_base", "horas_mensais_min", "horas_semanais_min", "data_desligamento");

    private final JdbcClient jdbc;

    public MudancaVinculoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Compara a área de preparação com os vínculos publicados, atributo por atributo. */
    public void registrarMudancasDaPreparacao(long cargaId) {
        for (String atributo : ATRIBUTOS) {
            jdbc.sql("""
                            INSERT INTO mudanca_vinculo (vinculo_id, carga_id, atributo, valor_anterior, valor_novo)
                            SELECT v.id, :carga, '%1$s', v.%1$s::text, s.%1$s::text
                              FROM vinculo v
                              JOIN vinculo_staging s ON s.entidade = v.entidade AND s.matricula = v.matricula
                             WHERE s.carga_id = :carga AND v.%1$s IS DISTINCT FROM s.%1$s""".formatted(atributo))
                    .param("carga", cargaId)
                    .update();
        }
    }

    /** Vínculos que estavam ausentes e voltaram a aparecer na fonte. */
    public void registrarRetornos(long cargaId) {
        jdbc.sql("""
                        INSERT INTO mudanca_vinculo (vinculo_id, carga_id, atributo, valor_anterior, valor_novo)
                        SELECT v.id, :carga, 'ativo_na_fonte', 'false', 'true'
                          FROM vinculo v
                          JOIN vinculo_staging s ON s.entidade = v.entidade AND s.matricula = v.matricula
                         WHERE s.carga_id = :carga AND NOT v.ativo_na_fonte""")
                .param("carga", cargaId)
                .update();
    }

    /** Vínculos presentes até agora que não vieram nesta carga. Chamar antes de marcá-los. */
    public void registrarAusencias(long cargaId) {
        jdbc.sql("""
                        INSERT INTO mudanca_vinculo (vinculo_id, carga_id, atributo, valor_anterior, valor_novo)
                        SELECT id, :carga, 'ativo_na_fonte', 'true', 'false'
                          FROM vinculo
                         WHERE ativo_na_fonte AND ultima_carga_id <> :carga""")
                .param("carga", cargaId)
                .update();
    }

    public int contarVinculosAlterados(long cargaId) {
        return jdbc.sql("SELECT count(DISTINCT vinculo_id) FROM mudanca_vinculo WHERE carga_id = :carga")
                .param("carga", cargaId).query(Integer.class).single();
    }
}
