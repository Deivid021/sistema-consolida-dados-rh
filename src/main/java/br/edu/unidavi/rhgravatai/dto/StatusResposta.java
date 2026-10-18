package br.edu.unidavi.rhgravatai.dto;

import java.time.OffsetDateTime;

/** Situação dos dados publicados (esquema Status do contrato; FR-013). */
public record StatusResposta(
        OffsetDateTime ultimaAtualizacao,
        long totalVinculos,
        long totalAtivos,
        String fonte,
        TetoResposta teto) {
}
