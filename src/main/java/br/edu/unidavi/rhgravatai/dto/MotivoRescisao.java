package br.edu.unidavi.rhgravatai.dto;

/** Quantidade de desligamentos por motivo; motivo nulo representa "Não informado". */
public record MotivoRescisao(String motivo, long quantidade) {
}
