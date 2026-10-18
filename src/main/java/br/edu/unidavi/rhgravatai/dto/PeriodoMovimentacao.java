package br.edu.unidavi.rhgravatai.dto;

/** Admissões e desligamentos de um mês ("yyyy-MM") ou ano ("yyyy"). */
public record PeriodoMovimentacao(String periodo, long admissoes, long desligamentos) {
}
