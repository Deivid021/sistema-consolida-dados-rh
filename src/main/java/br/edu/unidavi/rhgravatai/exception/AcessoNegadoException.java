package br.edu.unidavi.rhgravatai.exception;

public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Token de administração ausente ou inválido.");
    }
}
