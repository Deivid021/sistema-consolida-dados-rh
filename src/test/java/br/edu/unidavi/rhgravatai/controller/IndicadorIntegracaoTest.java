package br.edu.unidavi.rhgravatai.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

/** História 3: indicadores (RF03, FR-024 a FR-028), sobre dados/vinculos-exemplo.sql. */
@Sql("/dados/vinculos-exemplo.sql")
class IndicadorIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void quadroFechaComOTotal() throws Exception {
        mvc.perform(get("/api/v1/indicadores/quadro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.porSituacao[0].valor").value("TRABALHANDO"))
                .andExpect(jsonPath("$.porSituacao[0].quantidade").value(5))
                .andExpect(jsonPath("$.porSituacao[*].quantidade").value(contains(5, 2, 2)))
                .andExpect(jsonPath("$.porRegime[*].quantidade").value(contains(6, 2, 1)))
                .andExpect(jsonPath("$.porSecretaria[*].quantidade").value(contains(4, 3, 1, 1)))
                .andExpect(jsonPath("$.porSecretaria[3].valor").doesNotExist())
                .andExpect(jsonPath("$.ultimaAtualizacao").isNotEmpty());
    }

    @Test
    void quadroRespeitaFiltros() throws Exception {
        mvc.perform(get("/api/v1/indicadores/quadro").param("secretaria", "SMED - EDUCACAO"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.porSecretaria", hasSize(1)))
                .andExpect(jsonPath("$.porRegime[*].quantidade").value(contains(2, 1)));
    }

    @Test
    void salariosPorPadraoSoDeAtivos() throws Exception {
        mvc.perform(get("/api/v1/indicadores/salarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.itens[0].cargo").value("PROFESSOR"))
                .andExpect(jsonPath("$.itens[0].nivel").value("20"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].soma").value(8100.00))
                .andExpect(jsonPath("$.itens[0].media").value(4050.00))
                .andExpect(jsonPath("$.itens[1].cargo").value("ASSESSOR"))
                .andExpect(jsonPath("$.itens[1].nivel").doesNotExist());
    }

    @Test
    void salarioZeroOuAusenteFicaForaDaMedia() throws Exception {
        mvc.perform(get("/api/v1/indicadores/salarios").param("situacao", "DESLIGADO").param("ordenacao", "cargo,asc"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.itens[0].cargo").value("ENFERMEIRO"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(0))
                .andExpect(jsonPath("$.itens[0].quantidadeSemSalario").value(1))
                .andExpect(jsonPath("$.itens[0].soma").value(0))
                .andExpect(jsonPath("$.itens[0].media").doesNotExist())
                .andExpect(jsonPath("$.itens[1].cargo").value("MOTORISTA"))
                .andExpect(jsonPath("$.itens[1].quantidadeSemSalario").value(1));
    }

    @Test
    void situacaoExplicitaIncluiAusentesDaFonte() throws Exception {
        mvc.perform(get("/api/v1/indicadores/salarios").param("situacao", "TRABALHANDO").param("cargo", "ENFERMEIRO"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].soma").value(10200.00))
                .andExpect(jsonPath("$.itens[0].media").value(5100.00));
    }

    @Test
    void ordenacaoInvalidaRespondeErro() throws Exception {
        mvc.perform(get("/api/v1/indicadores/salarios").param("ordenacao", "salarioBase,desc"))
                .andExpect(status().isBadRequest());
    }
}
