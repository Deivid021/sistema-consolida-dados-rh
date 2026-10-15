package br.edu.unidavi.rhgravatai.vinculo;

/** Trechos de SQL com as definições de negócio usadas em mais de um lugar. */
public final class CondicoesVinculo {

    /**
     * Vínculo ativo (data-model.md): trabalhando ou afastado, e presente na última carga.
     * Usado nos totais de ativos, nos indicadores salariais (FR-026) e nos alertas (RF06).
     */
    public static final String ATIVO = "situacao IN ('TRABALHANDO', 'AFASTADO') AND ativo_na_fonte";

    private CondicoesVinculo() {
    }
}
