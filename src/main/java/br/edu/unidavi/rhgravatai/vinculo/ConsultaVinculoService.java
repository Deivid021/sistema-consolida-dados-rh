package br.edu.unidavi.rhgravatai.vinculo;

import br.edu.unidavi.rhgravatai.comum.PaginaResposta;
import br.edu.unidavi.rhgravatai.comum.Paginacao;
import br.edu.unidavi.rhgravatai.comum.UltimaAtualizacao;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos.CondicaoSql;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Busca e filtros de vínculos (RF02, FR-014 a FR-022). */
@Service
public class ConsultaVinculoService {

    /** Campos de ordenação do contrato e suas colunas. */
    public static final Map<String, String> ORDENACOES = ordenacoes();
    public static final String ORDENACAO_PADRAO = "nome,asc";

    private final JdbcClient jdbc;
    private final UltimaAtualizacao ultimaAtualizacao;

    public ConsultaVinculoService(JdbcClient jdbc, UltimaAtualizacao ultimaAtualizacao) {
        this.jdbc = jdbc;
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<VinculoResposta> consultar(FiltroVinculos filtro, Paginacao paginacao) {
        CondicaoSql condicao = filtro.paraSql("");
        long total = contar(condicao);
        List<VinculoResposta> itens = jdbc.sql("SELECT " + VinculoResposta.colunas("")
                        + " FROM vinculo WHERE " + condicao.where()
                        + " ORDER BY " + paginacao.orderBy() + ", id"
                        + " LIMIT :limite OFFSET :deslocamento")
                .params(condicao.parametros())
                .param("limite", paginacao.tamanho())
                .param("deslocamento", paginacao.deslocamento())
                .query(VinculoResposta::mapear)
                .list();
        return new PaginaResposta<>(itens, total, paginacao.pagina(), paginacao.tamanho(), ultimaAtualizacao.obter());
    }

    public long contar(FiltroVinculos filtro) {
        return contar(filtro.paraSql(""));
    }

    /**
     * Todos os vínculos do filtro, em fluxo, para a exportação (FR-034). Deve ser consumido
     * dentro de uma transação e fechado depois do uso.
     */
    public Stream<VinculoResposta> fluxo(FiltroVinculos filtro, String orderBy) {
        CondicaoSql condicao = filtro.paraSql("");
        return jdbc.sql("SELECT " + VinculoResposta.colunas("") + " FROM vinculo WHERE " + condicao.where()
                        + " ORDER BY " + orderBy + ", id")
                .params(condicao.parametros())
                .withFetchSize(1000)
                .query(VinculoResposta::mapear)
                .stream();
    }

    private long contar(CondicaoSql condicao) {
        return jdbc.sql("SELECT count(*) FROM vinculo WHERE " + condicao.where())
                .params(condicao.parametros())
                .query(Long.class)
                .single();
    }

    private static Map<String, String> ordenacoes() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("nome", "nome");
        mapa.put("matricula", "matricula");
        mapa.put("situacao", "situacao");
        mapa.put("regime", "regime");
        mapa.put("secretaria", "secretaria");
        mapa.put("cargo", "cargo");
        mapa.put("nivel", "nivel");
        mapa.put("salarioBase", "salario_base");
        mapa.put("dataAdmissao", "data_admissao");
        mapa.put("dataDesligamento", "data_desligamento");
        return Collections.unmodifiableMap(mapa);
    }
}
