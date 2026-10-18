package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.model.OrigemTeto;
import br.edu.unidavi.rhgravatai.model.Teto;
import java.math.BigDecimal;

/** Teto municipal em vigor na última carga (esquema Teto do contrato). */
public record TetoResposta(BigDecimal valor, OrigemTeto origem, String fundamento) {

    public static TetoResposta de(Teto teto) {
        return new TetoResposta(teto.valor(), teto.origem(), teto.fundamento());
    }
}
