package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Granularidade;
import br.edu.unidavi.rhgravatai.dto.MotivoRescisao;
import br.edu.unidavi.rhgravatai.dto.PeriodoMovimentacao;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Agregações dos gráficos de movimentação de pessoal (RF04). */
@Repository
public class GraficoRepository {

    private final JdbcClient jdbc;

    public GraficoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Admissões (por data de admissão) e desligamentos (por data de desligamento) em cada mês
     * ou ano entre as datas, incluindo os períodos sem movimentação (FR-030).
     */
    public List<PeriodoMovimentacao> movimentacao(Granularidade granularidade, LocalDate inicio, LocalDate fim,
                                                  FiltroVinculos filtro) {
        boolean mensal = granularidade == Granularidade.MES;
        FiltroSql sql = FiltroSql.de(filtro, "");
        return jdbc.sql("""
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
                         ORDER BY periodos.p""".formatted(sql.where()))
                .params(sql.com(Map.of(
                        "unidade", mensal ? "month" : "year",
                        "passo", mensal ? "1 month" : "1 year",
                        "formato", mensal ? "YYYY-MM" : "YYYY",
                        "inicio", inicio,
                        "fim", fim)))
                .query((rs, i) -> new PeriodoMovimentacao(rs.getString("periodo"), rs.getLong("admissoes"),
                        rs.getLong("desligamentos")))
                .list();
    }

    /** Desligamentos do período por motivo, do mais frequente para o menos frequente. */
    public List<MotivoRescisao> motivosDeDesligamento(LocalDate inicio, LocalDate fim, FiltroVinculos filtro) {
        FiltroSql sql = FiltroSql.de(filtro, "");
        return jdbc.sql("""
                        SELECT motivo_desligamento AS motivo, count(*) AS quantidade
                          FROM vinculo
                         WHERE situacao = 'DESLIGADO' AND data_desligamento BETWEEN :inicio AND :fim AND %s
                         GROUP BY motivo_desligamento
                         ORDER BY quantidade DESC, motivo_desligamento""".formatted(sql.where()))
                .params(sql.com(Map.of("inicio", inicio, "fim", fim)))
                .query((rs, i) -> new MotivoRescisao(rs.getString("motivo"), rs.getLong("quantidade")))
                .list();
    }
}
