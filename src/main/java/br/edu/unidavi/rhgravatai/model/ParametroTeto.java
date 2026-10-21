package br.edu.unidavi.rhgravatai.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valor manual do teto municipal, que prevalece sobre o valor da fonte a partir da vigência (FR-037). */
public record ParametroTeto(BigDecimal valor, LocalDate vigenciaInicio, String fundamento) {
}
