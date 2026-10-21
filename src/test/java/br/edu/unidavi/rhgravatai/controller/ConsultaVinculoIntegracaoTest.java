package br.edu.unidavi.rhgravatai.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

/** História 2: busca e filtros (RF02, FR-014 a FR-022), sobre dados/vinculos-exemplo.sql. */
@Sql("/dados/vinculos-exemplo.sql")
class ConsultaVinculoIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void semFiltrosListaTudoOrdenadoPorNome() throws Exception {
        consultar("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.ultimaAtualizacao").isNotEmpty())
                .andExpect(jsonPath("$.itens[0].nome").value("ANA SOUZA"))
                .andExpect(jsonPath("$.itens[0].salarioBase").value(4000.00))
                .andExpect(jsonPath("$.itens[0].horasMensais").value(200.00))
                .andExpect(jsonPath("$.itens[0].dataAdmissao").value("2015-02-10"));
    }

    @Test
    void filtrosDiferentesCombinamComE() throws Exception {
        consultar("situacao=AFASTADO&secretaria=SMED - EDUCACAO")
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].matricula").value("3-01"));
    }

    @Test
    void valoresDoMesmoFiltroCombinamComOu() throws Exception {
        consultar("regime=CLT&regime=ESTATUTARIO").andExpect(jsonPath("$.total").value(8));
        consultar("situacao=AFASTADO&situacao=DESLIGADO").andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void naoInformadoProcuraOsSemODado() throws Exception {
        consultar("classificacao=NAO_INFORMADO").andExpect(jsonPath("$.total").value(7));
        consultar("secretaria=NAO_INFORMADO")
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].matricula").value("8-01"));
        consultar("classificacao=PENSIONISTA&classificacao=NAO_INFORMADO").andExpect(jsonPath("$.total").value(9));
    }

    @Test
    void cadaFiltroIsolado() throws Exception {
        consultar("localTrabalho=UBS CENTRO").andExpect(jsonPath("$.total").value(1));
        consultar("cargo=PROFESSOR").andExpect(jsonPath("$.total").value(2));
        consultar("classificacao=PENSIONISTA").andExpect(jsonPath("$.total").value(2));
        consultar("regime=CARGO COMISSAO").andExpect(jsonPath("$.itens[0].matricula").value("6-01"));
    }

    @Test
    void buscaIgnoraMaiusculasEAcentos() throws Exception {
        consultar("q=jose").andExpect(jsonPath("$.itens[*].nome", containsInAnyOrder("JOSÉ DA SILVA")));
        consultar("q=JOÃO").andExpect(jsonPath("$.itens[*].matricula", containsInAnyOrder("3-01")));
        consultar("q=professor").andExpect(jsonPath("$.total").value(2));
        consultar("q=7-01").andExpect(jsonPath("$.itens[*].matricula", containsInAnyOrder("7-01")));
    }

    @Test
    void periodosIncluemAsDatasEAceitamSoUma() throws Exception {
        consultar("admissaoInicio=2025-01-01")
                .andExpect(jsonPath("$.itens[*].matricula", containsInAnyOrder("6-01", "7-01")));
        consultar("admissaoInicio=2025-01-02&admissaoFim=2025-01-02").andExpect(jsonPath("$.total").value(1));
        consultar("admissaoFim=1995-12-31").andExpect(jsonPath("$.itens[*].matricula", containsInAnyOrder("5-01")));
    }

    @Test
    void periodoDeDesligamentoRetornaSoDesligados() throws Exception {
        consultar("desligamentoInicio=2026-01-01")
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].matricula").value("4-01"))
                .andExpect(jsonPath("$.itens[0].situacao").value("DESLIGADO"))
                .andExpect(jsonPath("$.itens[0].motivoDesligamento").value("Exoneração"));
    }

    @Test
    void periodoInvertidoRespondeErroEmPortugues() throws Exception {
        consultar("admissaoInicio=2026-12-31&admissaoFim=2026-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("data inicial não pode ser maior")));
    }

    @Test
    void parametrosInvalidosRespondem400() throws Exception {
        consultar("situacao=APOSENTADO").andExpect(status().isBadRequest());
        consultar("admissaoInicio=31/12/2026").andExpect(status().isBadRequest());
        consultar("tamanho=101").andExpect(status().isBadRequest());
        consultar("ordenacao=cpf,asc").andExpect(status().isBadRequest());
        consultar("q=a").andExpect(status().isBadRequest());
    }

    @Test
    void paginaEOrdena() throws Exception {
        consultar("pagina=1&tamanho=5")
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.itens", hasSize(4)));
        consultar("ordenacao=salarioBase,desc&tamanho=1")
                .andExpect(jsonPath("$.itens[0].matricula").value("6-01"));
        consultar("ordenacao=salarioBase,asc&tamanho=1")
                .andExpect(jsonPath("$.itens[0].matricula").value("4-01"));
    }

    @Test
    void vinculoAusenteDaFonteApareceMarcado() throws Exception {
        consultar("q=servidor antigo")
                .andExpect(jsonPath("$.itens[0].ativoNaFonte").value(false));
    }

    @Test
    void opcoesDeFiltroComQuantidadesENaoInformado() throws Exception {
        mvc.perform(get("/api/v1/filtros/opcoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao", hasSize(3)))
                .andExpect(jsonPath("$.secretaria[?(@.valor == 'SMS - SAUDE')].quantidade").value(4))
                .andExpect(jsonPath("$.secretaria[3].valor").doesNotExist())
                .andExpect(jsonPath("$.secretaria[3].quantidade").value(1))
                .andExpect(jsonPath("$.localTrabalho", hasSize(5)));
    }

    private ResultActions consultar(String parametros) throws Exception {
        var requisicao = get("/api/v1/vinculos");
        if (!parametros.isEmpty()) {
            for (String par : parametros.split("&")) {
                String[] chaveValor = par.split("=", 2);
                requisicao = requisicao.param(chaveValor[0], chaveValor[1]);
            }
        }
        return mvc.perform(requisicao);
    }
}
