package br.edu.unidavi.rhgravatai.vinculo;

/** Trechos de SQL com as definições de negócio usadas em mais de um lugar. */
public final class CondicoesVinculo {

    /**
     * Vínculo ativo (data-model.md): trabalhando ou afastado, e presente na última carga.
     * Usado nos totais de ativos, nos indicadores salariais (FR-026) e nos alertas (RF06).
     */
    public static final String ATIVO = ativo("");

    private CondicoesVinculo() {
    }

    /** {@link #ATIVO} com o alias da tabela vinculo (ex.: "v."). */
    public static String ativo(String prefixo) {
        return "(" + prefixo + "situacao IN ('TRABALHANDO', 'AFASTADO') AND " + prefixo + "ativo_na_fonte)";
    }
}
