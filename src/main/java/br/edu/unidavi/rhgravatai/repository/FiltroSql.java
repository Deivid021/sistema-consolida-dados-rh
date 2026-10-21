package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte os filtros e a ordenação pedidos pelo usuário em SQL com parâmetros nomeados.
 * Regras (FR-015 a FR-019): filtros diferentes com E; valores do mesmo filtro com OU;
 * NAO_INFORMADO vira IS NULL; períodos incluem as duas datas; período de desligamento retorna
 * só desligados; a busca usa a coluna texto_busca, sem acentos.
 *
 * @param where condição para usar depois de WHERE
 */
record FiltroSql(String where, Map<String, Object> parametros) {

    /**
     * Vínculo ativo (data-model.md): trabalhando ou afastado, e presente na última carga.
     * Usado nos totais de ativos, nos indicadores salariais (FR-026) e nos alertas (RF06).
     */
    static String ativo(String prefixo) {
        return "(" + prefixo + "situacao IN ('TRABALHANDO', 'AFASTADO') AND " + prefixo + "ativo_na_fonte)";
    }

    /**
     * @param prefixo alias da tabela vinculo com ponto (ex.: "v.") ou vazio
     * @throws br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException se o filtro for inválido
     */
    static FiltroSql de(FiltroVinculos filtro, String prefixo) {
        filtro.validar();
        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.temBusca()) {
            condicoes.add(prefixo + "texto_busca LIKE :fq");
            parametros.put("fq", "%" + escaparLike(filtro.buscaNormalizada()) + "%");
        }
        if (!filtro.situacao().isEmpty()) {
            condicoes.add(prefixo + "situacao IN (:fsituacao)");
            parametros.put("fsituacao", filtro.situacao().stream().map(Enum::name).toList());
        }
        lista(condicoes, parametros, prefixo + "regime", "fregime", filtro.regime());
        lista(condicoes, parametros, prefixo + "classificacao", "fclassificacao", filtro.classificacao());
        lista(condicoes, parametros, prefixo + "secretaria", "fsecretaria", filtro.secretaria());
        lista(condicoes, parametros, prefixo + "local_trabalho", "flocal", filtro.localTrabalho());
        lista(condicoes, parametros, prefixo + "cargo", "fcargo", filtro.cargo());
        periodo(condicoes, parametros, prefixo + "data_admissao", "fadm", filtro.admissaoInicio(), filtro.admissaoFim());
        if (filtro.temPeriodoDeDesligamento()) {
            condicoes.add(prefixo + "situacao = 'DESLIGADO'");
            periodo(condicoes, parametros, prefixo + "data_desligamento", "fdes",
                    filtro.desligamentoInicio(), filtro.desligamentoFim());
        }
        return new FiltroSql(condicoes.isEmpty() ? "TRUE" : String.join(" AND ", condicoes), parametros);
    }

    /** Devolve uma cópia com mais uma condição, sem parâmetros novos. */
    FiltroSql e(String condicao) {
        return new FiltroSql(where + " AND " + condicao, parametros);
    }

    /**
     * ORDER BY a partir da paginação. {@code colunas} mapeia o campo do contrato para a coluna;
     * a paginação já garantiu que o campo é um dos permitidos.
     */
    static String orderBy(Paginacao paginacao, Map<String, String> colunas) {
        String coluna = colunas.get(paginacao.campo());
        if (coluna == null) {
            throw new IllegalArgumentException("Campo de ordenação sem coluna: " + paginacao.campo());
        }
        return coluna + (paginacao.crescente() ? " ASC" : " DESC") + " NULLS LAST";
    }

    private static void lista(List<String> condicoes, Map<String, Object> parametros,
                              String coluna, String nome, List<String> valores) {
        if (valores.isEmpty()) {
            return;
        }
        List<String> informados = valores.stream().filter(v -> !v.equals(FiltroVinculos.NAO_INFORMADO)).toList();
        List<String> alternativas = new ArrayList<>();
        if (!informados.isEmpty()) {
            alternativas.add(coluna + " IN (:" + nome + ")");
            parametros.put(nome, informados);
        }
        if (valores.contains(FiltroVinculos.NAO_INFORMADO)) {
            alternativas.add(coluna + " IS NULL");
        }
        condicoes.add("(" + String.join(" OR ", alternativas) + ")");
    }

    private static void periodo(List<String> condicoes, Map<String, Object> parametros,
                                String coluna, String nome, LocalDate inicio, LocalDate fim) {
        if (inicio != null) {
            condicoes.add(coluna + " >= :" + nome + "Inicio");
            parametros.put(nome + "Inicio", inicio);
        }
        if (fim != null) {
            condicoes.add(coluna + " <= :" + nome + "Fim");
            parametros.put(nome + "Fim", fim);
        }
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Parâmetros desta condição mais os extras da consulta. */
    Map<String, Object> com(Map<String, Object> extras) {
        Map<String, Object> todos = new HashMap<>(parametros);
        todos.putAll(extras);
        return todos;
    }
}
