package br.edu.unidavi.rhgravatai.dto;

/** Valor disponível num filtro. Valor nulo representa "Não informado" (filtrar com NAO_INFORMADO). */
public record OpcaoFiltro(String valor, long quantidade) {
}
