package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.dto.Contagem;
import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.GrupoSalarialResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.model.Situacao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Agregações dos indicadores de RH (RF03). A unidade de contagem é o vínculo (matrícula). */
@Repository
public class IndicadorRepository {

    /** Campos de ordenação do contrato para salários e suas colunas. */
    public static final Map<String, String> ORDENACOES_SALARIO = ordenacoesSalario();

    /** Agrupamentos do quadro de pessoal (FR-024). */
    public enum Agrupamento { SITUACAO, REGIME, SECRETARIA }

    private static final String SALARIOS = """
            SELECT cargo, nivel,
                   count(*) FILTER (WHERE salario_base > 0) AS quantidade,
                   count(*) FILTER (WHERE salario_base IS NULL OR salario_base = 0) AS sem_salario,
                   coalesce(sum(salario_base) FILTER (WHERE salario_base > 0), 0) AS soma,
                   round(avg(salario_base) FILTER (WHERE salario_base > 0), 2) AS media
              FROM vinculo
             WHERE %s
             GROUP BY cargo, nivel""";

    private final JdbcClient jdbc;

    public IndicadorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Contagem> contarPor(Agrupamento agrupamento, FiltroVinculos filtro) {
        String coluna = agrupamento.name().toLowerCase();
        FiltroSql sql = FiltroSql.de(filtro, "");
        return jdbc.sql("SELECT " + coluna + " AS valor, count(*) AS quantidade FROM vinculo WHERE " + sql.where()
                        + " GROUP BY " + coluna + " ORDER BY quantidade DESC, " + coluna + " NULLS LAST")
                .params(sql.parametros())
                .query((rs, i) -> new Contagem(rs.getString("valor"), rs.getLong("quantidade")))
                .list();
    }

    // ---- Salário base por cargo e nível (FR-025, FR-026) ----

    public List<GrupoSalarialResposta> salarios(FiltroVinculos filtro, Paginacao paginacao) {
        FiltroSql sql = filtroSalarial(filtro);
        return jdbc.sql(SALARIOS.formatted(sql.where())
                        + " ORDER BY " + FiltroSql.orderBy(paginacao, ORDENACOES_SALARIO)
                        + ", cargo NULLS LAST, nivel NULLS LAST LIMIT :limite OFFSET :deslocamento")
                .params(sql.com(Map.of("limite", paginacao.tamanho(), "deslocamento", paginacao.deslocamento())))
                .query(IndicadorRepository::mapearGrupo)
                .list();
    }

    public long contarGruposSalariais(FiltroVinculos filtro) {
        FiltroSql sql = filtroSalarial(filtro);
        return jdbc.sql("SELECT count(*) FROM (" + SALARIOS.formatted(sql.where()) + ") g")
                .params(sql.parametros()).query(Long.class).single();
    }

    /** Todos os grupos, para a exportação. Fechar o fluxo depois do uso. */
    public Stream<GrupoSalarialResposta> fluxoSalarios(FiltroVinculos filtro, Paginacao ordenacao) {
        FiltroSql sql = filtroSalarial(filtro);
        return jdbc.sql(SALARIOS.formatted(sql.where()) + " ORDER BY "
                        + FiltroSql.orderBy(ordenacao, ORDENACOES_SALARIO) + ", cargo NULLS LAST, nivel NULLS LAST")
                .params(sql.parametros())
                .query(IndicadorRepository::mapearGrupo)
                .stream();
    }

    /** Sem filtro de situação, só vínculos ativos (FR-026). */
    private static FiltroSql filtroSalarial(FiltroVinculos filtro) {
        if (!filtro.situacao().isEmpty()) {
            return FiltroSql.de(filtro, "");
        }
        return FiltroSql.de(filtro.comSituacao(List.of(Situacao.TRABALHANDO, Situacao.AFASTADO)), "")
                .e("ativo_na_fonte");
    }

    private static GrupoSalarialResposta mapearGrupo(ResultSet rs, int linha) throws SQLException {
        return new GrupoSalarialResposta(
                rs.getString("cargo"),
                rs.getString("nivel"),
                rs.getLong("quantidade"),
                rs.getLong("sem_salario"),
                rs.getBigDecimal("soma"),
                rs.getBigDecimal("media"));
    }

    private static Map<String, String> ordenacoesSalario() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("cargo", "cargo");
        mapa.put("nivel", "nivel");
        mapa.put("quantidade", "quantidade");
        mapa.put("soma", "soma");
        mapa.put("media", "media");
        return Collections.unmodifiableMap(mapa);
    }
}
