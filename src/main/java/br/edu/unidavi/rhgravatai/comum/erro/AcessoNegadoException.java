package br.edu.unidavi.rhgravatai.comum.erro;

public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Token de administração ausente ou inválido.");
    }
}
