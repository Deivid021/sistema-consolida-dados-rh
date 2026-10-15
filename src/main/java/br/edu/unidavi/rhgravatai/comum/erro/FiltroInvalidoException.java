package br.edu.unidavi.rhgravatai.comum.erro;

/** Parâmetro de consulta inválido. A mensagem é exibida ao usuário. */
public class FiltroInvalidoException extends RuntimeException {

    public FiltroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
