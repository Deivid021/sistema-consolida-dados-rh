package br.edu.unidavi.rhgravatai.importacao.fonte;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Página da API de Gravataí. Uma resposta de erro com HTTP 200 ({@code {"msg","sis","code"}})
 * chega aqui com {@code registros} nulo e é tratada como falha pelo cliente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginaFonte(
        String paginaAtual,
        Integer totalPaginas,
        Integer totalRegistros,
        List<RegistroFonte> registros) {
}
