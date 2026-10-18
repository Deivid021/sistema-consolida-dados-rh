package br.edu.unidavi.rhgravatai.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Registro convertido e pronto para a área de preparação. */
public record RegistroTratado(
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
        Map<String, String> valoresOriginais,
        String textoBusca) {
}
