package br.edu.unidavi.rhgravatai.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;

/** Front-end empacotado no jar: rotas do React respondem com o index.html (src/test/resources/static). */
class FrontEndIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void raizEncaminhaParaOIndex() throws Exception {
        // Página de boas-vindas do Spring Boot: forward para o index.html
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void rotaDoReactEntregaOIndex() throws Exception {
        mvc.perform(get("/servidores").param("situacao", "TRABALHANDO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Front de teste")));
    }

    @Test
    void arquivoInexistenteDa404() throws Exception {
        mvc.perform(get("/assets/nao-existe.js")).andExpect(status().isNotFound());
    }

    @Test
    void rotaInexistenteDaApiContinuaComErroDaApi() throws Exception {
        mvc.perform(get("/api/v1/nao-existe")).andExpect(status().isNotFound());
    }

    @Test
    void apiContinuaRespondendo() throws Exception {
        mvc.perform(get("/api/v1/status")).andExpect(status().isOk());
    }
}
