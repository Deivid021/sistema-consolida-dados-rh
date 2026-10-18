package br.edu.unidavi.rhgravatai.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Gráfico de admissões e desligamentos por período (FR-030). */
public record MovimentacaoResposta(
        Granularidade granularidade,
        List<PeriodoMovimentacao> periodos,
        OffsetDateTime ultimaAtualizacao) {
}
