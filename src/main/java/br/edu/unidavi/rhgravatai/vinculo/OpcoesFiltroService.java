package br.edu.unidavi.rhgravatai.vinculo;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valores disponíveis para cada filtro, vindos dos dados importados (FR-016). */
@Service
public class OpcoesFiltroService {

    private final JdbcClient jdbc;

    public OpcoesFiltroService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public OpcoesFiltro listar() {
        return new OpcoesFiltro(
                opcoes("situacao"),
                opcoes("regime"),
                opcoes("classificacao"),
                opcoes("secretaria"),
                opcoes("local_trabalho"),
                opcoes("cargo"));
    }

    /** {@code coluna} vem só das constantes acima, nunca do usuário. */
    private List<Opcao> opcoes(String coluna) {
        return jdbc.sql("SELECT " + coluna + " AS valor, count(*) AS quantidade FROM vinculo"
                        + " GROUP BY " + coluna + " ORDER BY " + coluna + " NULLS LAST")
                .query((rs, linha) -> new Opcao(rs.getString("valor"), rs.getLong("quantidade")))
                .list();
    }

    /** Valor nulo representa "Não informado"; para filtrar por ele, use NAO_INFORMADO. */
    public record Opcao(String valor, long quantidade) {
    }

    public record OpcoesFiltro(
            List<Opcao> situacao,
            List<Opcao> regime,
            List<Opcao> classificacao,
            List<Opcao> secretaria,
            List<Opcao> localTrabalho,
            List<Opcao> cargo) {
    }
}
