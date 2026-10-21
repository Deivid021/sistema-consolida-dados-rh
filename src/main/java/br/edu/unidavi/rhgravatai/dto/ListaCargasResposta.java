package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.model.Carga;
import java.util.List;

/** Cargas da mais recente para a mais antiga. */
public record ListaCargasResposta(List<Carga> itens, long total) {
}
