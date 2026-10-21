package br.edu.unidavi.rhgravatai.exception;

public class ExportacaoMuitoGrandeException extends RuntimeException {

    public ExportacaoMuitoGrandeException(long linhas, int maximo) {
        super("O relatório tem " + linhas + " linhas e o PDF aceita no máximo " + maximo
                + ". Use o formato CSV ou refine os filtros.");
    }
}
