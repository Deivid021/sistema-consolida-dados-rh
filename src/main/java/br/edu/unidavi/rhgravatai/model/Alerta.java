package br.edu.unidavi.rhgravatai.model;

import java.math.BigDecimal;

/**
 * Sinalização de um vínculo por uma regra de alerta (RF06). É um indicativo para verificação,
 * não uma afirmação de irregularidade.
 *
 * @param valorReferencia teto municipal ou mediana do grupo
 * @param grupoTamanho tamanho do grupo de comparação; só na regra FORA_PADRAO
 */
public record Alerta(
        RegraAlerta regra,
        Vinculo vinculo,
        BigDecimal valorVinculo,
        BigDecimal valorReferencia,
        BigDecimal diferenca,
        BigDecimal diferencaPercentual,
        Integer grupoTamanho) {
}
