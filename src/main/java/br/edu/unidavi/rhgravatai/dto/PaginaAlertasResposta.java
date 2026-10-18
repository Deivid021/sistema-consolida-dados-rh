package br.edu.unidavi.rhgravatai.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Página de alertas com o teto e o percentual em vigor (esquema PaginaAlertas do contrato). */
public record PaginaAlertasResposta(
        List<AlertaResposta> itens,
        long total,
        int pagina,
        int tamanho,
        TetoResposta teto,
        BigDecimal percentualForaPadrao,
        OffsetDateTime ultimaAtualizacao) {
}
