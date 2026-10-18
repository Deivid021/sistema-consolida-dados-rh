package br.edu.unidavi.rhgravatai.model;

import java.math.BigDecimal;

/**
 * Teto municipal definido para uma carga (FR-037).
 *
 * @param valor nulo quando a regra está suspensa
 * @param prefeitoId vínculo do prefeito, que nunca é sinalizado; nulo se não houver
 */
public record Teto(BigDecimal valor, OrigemTeto origem, String fundamento, Long prefeitoId) {

    public static Teto suspenso() {
        return new Teto(null, OrigemTeto.SUSPENSA, null, null);
    }
}
