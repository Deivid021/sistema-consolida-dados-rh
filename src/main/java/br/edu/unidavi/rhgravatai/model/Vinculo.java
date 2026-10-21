package br.edu.unidavi.rhgravatai.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vínculo funcional (servidor/matrícula), identificado por entidade e matrícula (FR-008).
 * Corresponde à tabela vinculo; não existe CPF (princípio I da constituição).
 */
public record Vinculo(
        long id,
        String entidade,
        String matricula,
        String nome,
        Situacao situacao,
        LocalDate dataAdmissao,
        LocalDate dataDesligamento,
        String motivoDesligamento,
        String regime,
        String classificacao,
        String formaInvestidura,
        String secretaria,
        String localTrabalho,
        String cargo,
        String funcao,
        String nivel,
        BigDecimal salarioBase,
        String horario,
        Integer horasMensaisMin,
        Integer horasSemanaisMin,
        boolean ativoNaFonte) {
}
