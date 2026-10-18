package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.model.Situacao;
import br.edu.unidavi.rhgravatai.model.Vinculo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Vínculo como aparece na API (esquema Vinculo do contrato; FR-020). */
public record VinculoResposta(
        String matricula,
        String nome,
        Situacao situacao,
        String regime,
        String classificacao,
        String secretaria,
        String localTrabalho,
        String cargo,
        String funcao,
        String nivel,
        BigDecimal salarioBase,
        BigDecimal horasMensais,
        LocalDate dataAdmissao,
        LocalDate dataDesligamento,
        String motivoDesligamento,
        boolean ativoNaFonte) {

    public static VinculoResposta de(Vinculo v) {
        BigDecimal horas = v.horasMensaisMin() == null ? null
                : BigDecimal.valueOf(v.horasMensaisMin()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return new VinculoResposta(v.matricula(), v.nome(), v.situacao(), v.regime(), v.classificacao(),
                v.secretaria(), v.localTrabalho(), v.cargo(), v.funcao(), v.nivel(), v.salarioBase(), horas,
                v.dataAdmissao(), v.dataDesligamento(), v.motivoDesligamento(), v.ativoNaFonte());
    }
}
