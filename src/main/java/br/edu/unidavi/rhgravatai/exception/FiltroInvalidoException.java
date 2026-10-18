package br.edu.unidavi.rhgravatai.exception;

/** Parâmetro de consulta inválido. A mensagem é exibida ao usuário. */
public class FiltroInvalidoException extends RuntimeException {

    public FiltroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
