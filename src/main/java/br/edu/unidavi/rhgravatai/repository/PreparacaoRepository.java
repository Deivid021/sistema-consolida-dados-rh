package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.model.RegistroTratado;
import br.edu.unidavi.rhgravatai.model.Rejeicao;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Área de preparação (vinculo_staging) e registros rejeitados de uma carga. */
@Repository
public class PreparacaoRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;
    private final NamedParameterJdbcTemplate template;

    public PreparacaoRepository(JdbcClient jdbc, NamedParameterJdbcTemplate template) {
        this.jdbc = jdbc;
        this.template = template;
    }

    public void limpar() {
        jdbc.sql("DELETE FROM vinculo_staging").update();
    }

    public void inserir(long cargaId, int pagina, List<RegistroTratado> registros) {
        if (registros.isEmpty()) {
            return;
        }
        SqlParameterSource[] lote = registros.stream()
                .map(r -> new MapSqlParameterSource()
                        .addValue("carga", cargaId)
                        .addValue("pagina", pagina)
                        .addValue("entidade", r.entidade())
                        .addValue("matricula", r.matricula())
                        .addValue("nome", r.nome())
                        .addValue("situacao", r.situacao().name())
                        .addValue("dataAdmissao", r.dataAdmissao())
                        .addValue("dataDesligamento", r.dataDesligamento())
                        .addValue("motivo", r.motivoDesligamento())
                        .addValue("regime", r.regime())
                        .addValue("classificacao", r.classificacao())
                        .addValue("formaInvestidura", r.formaInvestidura())
                        .addValue("secretaria", r.secretaria())
                        .addValue("localTrabalho", r.localTrabalho())
                        .addValue("cargo", r.cargo())
                        .addValue("funcao", r.funcao())
                        .addValue("nivel", r.nivel())
                        .addValue("salario", r.salarioBase())
                        .addValue("horario", r.horario())
                        .addValue("horasMensais", r.horasMensaisMin())
                        .addValue("horasSemanais", r.horasSemanaisMin())
                        .addValue("originais", JSON.writeValueAsString(r.valoresOriginais()))
                        .addValue("textoBusca", r.textoBusca()))
                .toArray(SqlParameterSource[]::new);
        template.batchUpdate("""
                INSERT INTO vinculo_staging (
                    carga_id, pagina, entidade, matricula, nome, situacao, data_admissao,
                    data_desligamento, motivo_desligamento, regime, classificacao, forma_investidura,
                    secretaria, local_trabalho, cargo, funcao, nivel, salario_base, horario,
                    horas_mensais_min, horas_semanais_min, valores_originais, texto_busca)
                VALUES (
                    :carga, :pagina, :entidade, :matricula, :nome, :situacao, :dataAdmissao,
                    :dataDesligamento, :motivo, :regime, :classificacao, :formaInvestidura,
                    :secretaria, :localTrabalho, :cargo, :funcao, :nivel, :salario, :horario,
                    :horasMensais, :horasSemanais, CAST(:originais AS jsonb), :textoBusca)""", lote);
    }

    public void inserirRejeitados(long cargaId, int pagina, List<Rejeicao> rejeicoes) {
        if (rejeicoes.isEmpty()) {
            return;
        }
        SqlParameterSource[] lote = rejeicoes.stream()
                .map(r -> new MapSqlParameterSource()
                        .addValue("carga", cargaId)
                        .addValue("pagina", pagina)
                        .addValue("matricula", r.matricula())
                        .addValue("campo", r.campo())
                        .addValue("valor", r.valorRecebido())
                        .addValue("motivo", r.motivo()))
                .toArray(SqlParameterSource[]::new);
        template.batchUpdate("""
                INSERT INTO registro_rejeitado (carga_id, pagina, matricula, campo, valor_recebido, motivo)
                VALUES (:carga, :pagina, :matricula, :campo, :valor, :motivo)""", lote);
    }

    public int contarLinhas(long cargaId) {
        return jdbc.sql("SELECT count(*) FROM vinculo_staging WHERE carga_id = :carga")
                .param("carga", cargaId).query(Integer.class).single();
    }

    public int contarVinculosDistintos(long cargaId) {
        return jdbc.sql("""
                        SELECT count(*) FROM (
                            SELECT DISTINCT entidade, matricula FROM vinculo_staging WHERE carga_id = :carga) d""")
                .param("carga", cargaId).query(Integer.class).single();
    }

    public int contarRejeitados(long cargaId) {
        return jdbc.sql("SELECT count(*) FROM registro_rejeitado WHERE carga_id = :carga")
                .param("carga", cargaId).query(Integer.class).single();
    }
}
