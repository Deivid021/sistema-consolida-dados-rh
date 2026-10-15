package br.edu.unidavi.rhgravatai.grafico;

import br.edu.unidavi.rhgravatai.comum.UltimaAtualizacao;
import br.edu.unidavi.rhgravatai.comum.erro.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos.CondicaoSql;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados dos gráficos de movimentação (RF04). A distribuição por regime e secretaria vem do
 * {@code /indicadores/quadro}.
 */
@Service
public class GraficoService {

    static final int MAXIMO_MOTIVOS = 10;
    static final String OUTROS = "Outros";
    static final String NAO_INFORMADO = "Não informado";

    private final JdbcClient jdbc;
    private final UltimaAtualizacao ultimaAtualizacao;

    public GraficoService(JdbcClient jdbc, UltimaAtualizacao ultimaAtualizacao) {
        this.jdbc = jdbc;
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    /**
     * Admissões (por data de admissão) e desligamentos (por data de desligamento) em cada mês
     * ou ano do período, incluindo os períodos sem movimentação (FR-030).
     */
    @Transactional(readOnly = true)
    public Movimentacao movimentacao(Granularidade granularidade, LocalDate inicio, LocalDate fim, FiltroVinculos filtro) {
        validar(inicio, fim);
        CondicaoSql condicao = filtro.paraSql("");
        Map<String, Object> parametros = new HashMap<>(condicao.parametros());
        parametros.put("unidade", granularidade.unidadeSql);
        parametros.put("passo", granularidade.passoSql);
        parametros.put("formato", granularidade.formatoSql);
        parametros.put("inicio", inicio);
        parametros.put("fim", fim);

        List<Periodo> periodos = jdbc.sql("""
                        WITH periodos AS (
                            SELECT generate_series(date_trunc(:unidade, CAST(:inicio AS timestamp)),
                                                   date_trunc(:unidade, CAST(:fim AS timestamp)),
                                                   CAST(:passo AS interval)) AS p
                        ),
                        admissoes AS (
                            SELECT date_trunc(:unidade, CAST(data_admissao AS timestamp)) AS p, count(*) AS n
                              FROM vinculo
                             WHERE data_admissao BETWEEN :inicio AND :fim AND %1$s
                             GROUP BY 1
                        ),
                        desligamentos AS (
                            SELECT date_trunc(:unidade, CAST(data_desligamento AS timestamp)) AS p, count(*) AS n
                              FROM vinculo
                             WHERE situacao = 'DESLIGADO' AND data_desligamento BETWEEN :inicio AND :fim AND %1$s
                             GROUP BY 1
                        )
                        SELECT to_char(periodos.p, :formato) AS periodo,
                               coalesce(admissoes.n, 0) AS admissoes,
                               coalesce(desligamentos.n, 0) AS desligamentos
                          FROM periodos
                          LEFT JOIN admissoes USING (p)
                          LEFT JOIN desligamentos USING (p)
                         ORDER BY periodos.p""".formatted(condicao.where()))
                .params(parametros)
                .query((rs, i) -> new Periodo(rs.getString("periodo"), rs.getLong("admissoes"), rs.getLong("desligamentos")))
                .list();
        return new Movimentacao(granularidade, periodos, ultimaAtualizacao.obter());
    }

    /**
     * Os dez motivos de desligamento mais frequentes no período; os demais somados em
     * "Outros" e os ausentes em "Não informado" (FR-031).
     */
    @Transactional(readOnly = true)
    public MotivosRescisao motivos(LocalDate inicio, LocalDate fim, FiltroVinculos filtro) {
        validar(inicio, fim);
        CondicaoSql condicao = filtro.paraSql("");
        List<Motivo> grupos = jdbc.sql("""
                        SELECT motivo_desligamento AS motivo, count(*) AS quantidade
                          FROM vinculo
                         WHERE situacao = 'DESLIGADO' AND data_desligamento BETWEEN :inicio AND :fim AND %s
                         GROUP BY motivo_desligamento
                         ORDER BY quantidade DESC, motivo_desligamento""".formatted(condicao.where()))
                .params(condicao.parametros())
                .param("inicio", inicio)
                .param("fim", fim)
                .query((rs, i) -> new Motivo(rs.getString("motivo"), rs.getLong("quantidade")))
                .list();

        List<Motivo> motivos = new ArrayList<>();
        long outros = 0;
        long naoInformado = 0;
        long total = 0;
        for (Motivo grupo : grupos) {
            total += grupo.quantidade();
            if (grupo.motivo() == null) {
                naoInformado += grupo.quantidade();
            } else if (motivos.size() < MAXIMO_MOTIVOS) {
                motivos.add(grupo);
            } else {
                outros += grupo.quantidade();
            }
        }
        if (outros > 0) {
            motivos.add(new Motivo(OUTROS, outros));
        }
        if (naoInformado > 0) {
            motivos.add(new Motivo(NAO_INFORMADO, naoInformado));
        }
        return new MotivosRescisao(total, motivos, ultimaAtualizacao.obter());
    }

    private static void validar(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new FiltroInvalidoException("A data inicial não pode ser maior que a data final.");
        }
    }

    public enum Granularidade {
        MES("month", "1 month", "YYYY-MM"),
        ANO("year", "1 year", "YYYY");

        private final String unidadeSql;
        private final String passoSql;
        private final String formatoSql;

        Granularidade(String unidadeSql, String passoSql, String formatoSql) {
            this.unidadeSql = unidadeSql;
            this.passoSql = passoSql;
            this.formatoSql = formatoSql;
        }
    }

    public record Periodo(String periodo, long admissoes, long desligamentos) {
    }

    public record Movimentacao(Granularidade granularidade, List<Periodo> periodos, OffsetDateTime ultimaAtualizacao) {
    }

    public record Motivo(String motivo, long quantidade) {
    }

    public record MotivosRescisao(long total, List<Motivo> motivos, OffsetDateTime ultimaAtualizacao) {
    }
}
