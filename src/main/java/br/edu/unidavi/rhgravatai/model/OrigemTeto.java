package br.edu.unidavi.rhgravatai.model;

/** De onde veio o teto municipal usado nos alertas (FR-037). */
public enum OrigemTeto {
    /** Salário base do vínculo ativo de cargo PREFEITO publicado pela fonte. */
    FONTE,
    /** Valor configurado em parametro_teto. */
    MANUAL,
    /** Sem prefeito ativo nem valor configurado: a regra não é aplicada. */
    SUSPENSA
}
