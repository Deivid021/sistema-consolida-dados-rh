package br.edu.unidavi.rhgravatai.indicador;

import br.edu.unidavi.rhgravatai.comum.UltimaAtualizacao;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos.CondicaoSql;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quantidade de vínculos por situação, regime e secretaria (RF03, FR-024, FR-027). Os mesmos
 * números alimentam os gráficos de distribuição (FR-029). A unidade é o vínculo (matrícula).
 */
@Service
public class QuadroService {

    private final JdbcClient jdbc;
    private final UltimaAtualizacao ultimaAtualizacao;

    public QuadroService(JdbcClient jdbc, UltimaAtualizacao ultimaAtualizacao) {
        this.jdbc = jdbc;
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    @Transactional(readOnly = true)
    public Quadro calcular(FiltroVinculos filtro) {
        CondicaoSql condicao = filtro.paraSql("");
        long total = jdbc.sql("SELECT count(*) FROM vinculo WHERE " + condicao.where())
                .params(condicao.parametros()).query(Long.class).single();
        return new Quadro(
                total,
                agrupar("situacao", condicao),
                agrupar("regime", condicao),
                agrupar("secretaria", condicao),
                ultimaAtualizacao.obter());
    }

    /** {@code coluna} vem só das constantes acima, nunca do usuário. */
    private List<Contagem> agrupar(String coluna, CondicaoSql condicao) {
        return jdbc.sql("SELECT " + coluna + " AS valor, count(*) AS quantidade FROM vinculo WHERE " + condicao.where()
                        + " GROUP BY " + coluna + " ORDER BY quantidade DESC, " + coluna + " NULLS LAST")
                .params(condicao.parametros())
                .query((rs, i) -> new Contagem(rs.getString("valor"), rs.getLong("quantidade")))
                .list();
    }

    public record Quadro(
            long total,
            List<Contagem> porSituacao,
            List<Contagem> porRegime,
            List<Contagem> porSecretaria,
            OffsetDateTime ultimaAtualizacao) {
    }
}
