package br.edu.unidavi.rhgravatai.dto;

import java.util.List;

/** Valores de cada filtro com a quantidade de vínculos (FR-016). */
public record OpcoesFiltroResposta(
        List<OpcaoFiltro> situacao,
        List<OpcaoFiltro> regime,
        List<OpcaoFiltro> classificacao,
        List<OpcaoFiltro> secretaria,
        List<OpcaoFiltro> localTrabalho,
        List<OpcaoFiltro> cargo) {
}
