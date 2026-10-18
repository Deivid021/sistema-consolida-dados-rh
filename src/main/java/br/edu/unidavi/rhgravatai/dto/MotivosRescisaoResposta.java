package br.edu.unidavi.rhgravatai.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Dez motivos mais frequentes, mais "Outros" e "Não informado" (FR-031). */
public record MotivosRescisaoResposta(long total, List<MotivoRescisao> motivos, OffsetDateTime ultimaAtualizacao) {
}
