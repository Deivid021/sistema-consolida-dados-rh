package br.edu.unidavi.rhgravatai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Registro como vem da API de Gravataí. Todos os campos são texto e são convertidos no
 * tratamento.
 *
 * <p><b>Não existe campo {@code cpf}</b>: o CPF enviado pela fonte é descartado já na
 * leitura do JSON e nunca chega a existir dentro do sistema (princípio I da constituição,
 * RNF01). Não adicione esse campo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistroFonte(
        String entidade,
        String matricula,
        String nome,
        String situacao,
        String admissao,
        String dataRescisao,
        String motivoRescisao,
        String regime,
        String classificacao,
        String formaInvestidura,
        String centroCusto,
        String localTrabalho,
        String cargo,
        String funcao,
        String nivel,
        String salarioBase,
        String horario,
        String horasMensais,
        String horasSemanais) {
}
