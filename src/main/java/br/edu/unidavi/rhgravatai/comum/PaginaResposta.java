package br.edu.unidavi.rhgravatai.comum;

import java.time.OffsetDateTime;
import java.util.List;

/** Página de resultados no formato do contrato (itens, total, pagina, tamanho, ultimaAtualizacao). */
public record PaginaResposta<T>(
        List<T> itens,
        long total,
        int pagina,
        int tamanho,
        OffsetDateTime ultimaAtualizacao) {
}
