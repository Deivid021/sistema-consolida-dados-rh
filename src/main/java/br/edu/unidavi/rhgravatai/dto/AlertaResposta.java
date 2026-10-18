package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import java.math.BigDecimal;

/**
 * Alerta como aparece na API (FR-040). {@code descricaoRegra} explica o critério e é exibida
 * junto ao aviso de que é uma sinalização para verificação (FR-041).
 */
public record AlertaResposta(
        RegraAlerta regra,
        VinculoResposta vinculo,
        BigDecimal valorVinculo,
        BigDecimal valorReferencia,
        BigDecimal diferenca,
        BigDecimal diferencaPercentual,
        Integer grupoTamanho,
        String descricaoRegra) {
}
