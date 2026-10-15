package br.edu.unidavi.rhgravatai.importacao.carga;

/**
 * Etapa executada dentro da transação de publicação, depois que os vínculos foram
 * atualizados (ex.: recálculo dos alertas, FR-041). Uma exceção aqui desfaz a publicação.
 */
public interface AposPublicacao {

    void aposPublicar(long cargaId);
}
