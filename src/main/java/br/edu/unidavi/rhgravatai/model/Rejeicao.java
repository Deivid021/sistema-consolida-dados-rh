package br.edu.unidavi.rhgravatai.model;

/**
 * Registro que não passou no tratamento (FR-007). {@code valorRecebido} é só o valor do campo
 * com problema, nunca o registro inteiro.
 */
public record Rejeicao(String matricula, String campo, String valorRecebido, String motivo) {
}
