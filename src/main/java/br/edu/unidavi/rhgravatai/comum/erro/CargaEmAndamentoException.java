package br.edu.unidavi.rhgravatai.comum.erro;

public class CargaEmAndamentoException extends RuntimeException {

    public CargaEmAndamentoException() {
        super("Já existe uma carga em andamento. Aguarde ela terminar.");
    }
}
