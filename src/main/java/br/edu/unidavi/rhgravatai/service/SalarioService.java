package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos.CondicaoSql;
import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.model.Situacao;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quantidade, soma e média do salário base por cargo e nível (RF03, FR-025, FR-026).
 *
 * <p>Sem filtro de situação, considera só vínculos ativos (trabalhando ou afastado e presentes
 * na última carga). Salário zero ou ausente fica fora da soma e da média e é contado à parte.
 */
@Service
public class SalarioService {

    public static final Map<String, String> ORDENACOES = ordenacoes();
    public static final String ORDENACAO_PADRAO = "soma,desc";

    private static final String AGREGACAO = """
            SELECT cargo, nivel,
                   count(*) FILTER (WHERE salario_base > 0) AS quantidade,
                   count(*) FILTER (WHERE salario_base IS NULL OR salario_base = 0) AS sem_salario,
                   coalesce(sum(salario_base) FILTER (WHERE salario_base > 0), 0) AS soma,
                   round(avg(salario_base) FILTER (WHERE salario_base > 0), 2) AS media
              FROM vinculo
             WHERE %s
             GROUP BY cargo, nivel""";

    private final JdbcClient jdbc;
    private final UltimaAtualizacao ultimaAtualizacao;

    public SalarioService(JdbcClient jdbc, UltimaAtualizacao ultimaAtualizacao) {
        this.jdbc = jdbc;
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<GrupoSalarial> calcular(FiltroVinculos filtro, Paginacao paginacao) {
        CondicaoSql condicao = condicao(filtro);
        long total = jdbc.sql("SELECT count(*) FROM (" + AGREGACAO.formatted(condicao.where()) + ") g")
                .params(condicao.parametros()).query(Long.class).single();
        List<GrupoSalarial> itens = jdbc.sql(AGREGACAO.formatted(condicao.where())
                        + " ORDER BY " + paginacao.orderBy() + ", cargo NULLS LAST, nivel NULLS LAST"
                        + " LIMIT :limite OFFSET :deslocamento")
                .params(condicao.parametros())
                .param("limite", paginacao.tamanho())
                .param("deslocamento", paginacao.deslocamento())
                .query(SalarioService::mapear)
                .list();
        return new PaginaResposta<>(itens, total, paginacao.pagina(), paginacao.tamanho(), ultimaAtualizacao.obter());
    }

    /** Todos os grupos, em fluxo, para a exportação. Consumir dentro de uma transação. */
    public Stream<GrupoSalarial> fluxo(FiltroVinculos filtro, String orderBy) {
        CondicaoSql condicao = condicao(filtro);
        return jdbc.sql(AGREGACAO.formatted(condicao.where()) + " ORDER BY " + orderBy
                        + ", cargo NULLS LAST, nivel NULLS LAST")
                .params(condicao.parametros())
                .query(SalarioService::mapear)
                .stream();
    }

    public long contarGrupos(FiltroVinculos filtro) {
        CondicaoSql condicao = condicao(filtro);
        return jdbc.sql("SELECT count(*) FROM (" + AGREGACAO.formatted(condicao.where()) + ") g")
                .params(condicao.parametros()).query(Long.class).single();
    }

    private static CondicaoSql condicao(FiltroVinculos filtro) {
        if (!filtro.situacao().isEmpty()) {
            return filtro.paraSql("");
        }
        CondicaoSql base = filtro.comSituacao(List.of(Situacao.TRABALHANDO, Situacao.AFASTADO)).paraSql("");
        return new CondicaoSql(base.where() + " AND ativo_na_fonte", base.parametros());
    }

    private static GrupoSalarial mapear(ResultSet rs, int linha) throws SQLException {
        return new GrupoSalarial(
                rs.getString("cargo"),
                rs.getString("nivel"),
                rs.getLong("quantidade"),
                rs.getLong("sem_salario"),
                rs.getBigDecimal("soma"),
                rs.getBigDecimal("media"));
    }

    private static Map<String, String> ordenacoes() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("cargo", "cargo");
        mapa.put("nivel", "nivel");
        mapa.put("quantidade", "quantidade");
        mapa.put("soma", "soma");
        mapa.put("media", "media");
        return Collections.unmodifiableMap(mapa);
    }

    public record GrupoSalarial(
            String cargo,
            String nivel,
            long quantidade,
            long quantidadeSemSalario,
            BigDecimal soma,
            BigDecimal media) {
    }
}
