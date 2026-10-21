package br.edu.unidavi.rhgravatai.dto;

import java.math.BigDecimal;

/**
 * Salário base de um grupo de cargo e nível (FR-025, FR-026).
 *
 * @param quantidade vínculos com salário maior que zero, que entram na soma e na média
 * @param quantidadeSemSalario vínculos com salário zero ou ausente
 * @param media nula quando nenhum vínculo do grupo tem salário
 */
public record GrupoSalarialResposta(
        String cargo,
        String nivel,
        long quantidade,
        long quantidadeSemSalario,
        BigDecimal soma,
        BigDecimal media) {
}
