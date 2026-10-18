package br.edu.unidavi.rhgravatai.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Quantidade de vínculos por situação, regime e secretaria (FR-024, FR-029). */
public record QuadroResposta(
        long total,
        List<Contagem> porSituacao,
        List<Contagem> porRegime,
        List<Contagem> porSecretaria,
        OffsetDateTime ultimaAtualizacao) {
}
