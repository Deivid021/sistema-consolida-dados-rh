package br.edu.unidavi.rhgravatai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;

/** História 6: exportação em CSV e PDF (RF05, FR-033 a FR-036), sobre dados/vinculos-exemplo.sql. */
@Sql("/dados/vinculos-exemplo.sql")
class ExportacaoIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void csvTrazTodasAsLinhasNoPadraoBrasileiro() throws Exception {
        inserirVinculos(25);

        MockHttpServletResponse resposta = exportar("/api/v1/exportacoes/vinculos?formato=csv");

        assertThat(resposta.getStatus()).isEqualTo(200);
        assertThat(resposta.getContentType()).startsWith("text/csv");
        assertThat(resposta.getHeader("Content-Disposition")).matches("attachment; filename=\"vinculos-\\d{4}-\\d{2}-\\d{2}\\.csv\"");
        byte[] bytes = resposta.getContentAsByteArray();
        assertThat(bytes).startsWith(0xEF, 0xBB, 0xBF);

        List<String> linhas = linhas(resposta);
        assertThat(linhas).hasSize(1 + 9 + 25);
        assertThat(linhas.getFirst()).startsWith("﻿Matrícula;Nome;Situação;Regime;Classificação;Secretaria;");
        assertThat(linhas).contains(
                "2-01;ANA SOUZA;TRABALHANDO;ESTATUTARIO;;SMED - EDUCACAO;EMEF A;PROFESSOR;;20;4000,00;200,00;10/02/2015;;;Sim");
        assertThat(String.join("\n", linhas)).doesNotContainPattern("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\*\\*\\*\\.");
    }

    @Test
    void csvRespeitaFiltrosEOrdenacao() throws Exception {
        List<String> linhas = linhas(exportar(
                "/api/v1/exportacoes/vinculos?formato=csv&situacao=AFASTADO&ordenacao=salarioBase,desc"));

        assertThat(linhas).hasSize(3);
        assertThat(linhas.get(1)).startsWith("3-01;");
        assertThat(linhas.get(2)).startsWith("8-01;");
    }

    @Test
    void pdfComTituloFiltrosTotaisEFonte() throws Exception {
        MockHttpServletResponse resposta = exportar("/api/v1/exportacoes/vinculos?formato=pdf&secretaria=GP - GABINETE");

        assertThat(resposta.getStatus()).isEqualTo(200);
        assertThat(resposta.getContentType()).isEqualTo("application/pdf");
        String texto = textoDoPdf(resposta.getContentAsByteArray());
        assertThat(texto).contains("Consulta de v", "Secretaria: GP - GABINETE", "Dados atualizados em: 15/10/2026 02:15",
                "Total de v", ": 1", "LÚCIA COSTA", "R$ 8.000,00", "Fonte: API de Dados Abertos de Pessoal");
    }

    @Test
    void pdfAcimaDoLimiteERecusado() throws Exception {
        inserirVinculos(5000);

        mvc.perform(get("/api/v1/exportacoes/vinculos").param("formato", "pdf"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "O relatório tem 5009 linhas e o PDF aceita no máximo 5000. Use o formato CSV ou refine os filtros."));
        mvc.perform(get("/api/v1/exportacoes/vinculos").param("formato", "csv"))
                .andExpect(status().isOk());
    }

    @Test
    void outrosRelatorios() throws Exception {
        assertThat(linhas(exportar("/api/v1/exportacoes/quadro?formato=csv")))
                .contains("Situação;TRABALHANDO;5", "Secretaria;Não informado;1");
        assertThat(linhas(exportar("/api/v1/exportacoes/salarios?formato=csv")).get(1))
                .isEqualTo("PROFESSOR;20;2;0;8100,00;4050,00");
        assertThat(linhas(exportar("/api/v1/exportacoes/alertas?formato=csv")).getFirst())
                .contains("Regra;Matrícula;Nome;Cargo;Nível;Secretaria;Salário base;Referência;Diferença (%);Critério");
        assertThat(textoDoPdf(exportar("/api/v1/exportacoes/salarios?formato=pdf").getContentAsByteArray()))
                .contains("Somente v", "PROFESSOR", "R$ 4.050,00");
    }

    @Test
    void parametrosInvalidos() throws Exception {
        mvc.perform(get("/api/v1/exportacoes/folha").param("formato", "csv")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/exportacoes/vinculos").param("formato", "xlsx")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/exportacoes/vinculos")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/exportacoes/vinculos").param("formato", "csv").param("ordenacao", "x,asc"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletResponse exportar(String url) throws Exception {
        String[] partes = url.split("\\?", 2);
        var requisicao = get(partes[0]);
        for (String par : partes[1].split("&")) {
            String[] chaveValor = par.split("=", 2);
            requisicao = requisicao.param(chaveValor[0], chaveValor[1]);
        }
        return mvc.perform(requisicao).andReturn().getResponse();
    }

    private static List<String> linhas(MockHttpServletResponse resposta) throws Exception {
        return resposta.getContentAsString(StandardCharsets.UTF_8).lines().toList();
    }

    private static String textoDoPdf(byte[] pdf) throws Exception {
        PdfReader leitor = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= leitor.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(leitor).getTextFromPage(pagina)).append('\n');
        }
        return texto.toString();
    }

    private void inserirVinculos(int quantidade) {
        jdbc.sql("""
                        INSERT INTO vinculo (entidade, matricula, nome, situacao, cargo, salario_base, texto_busca)
                        SELECT 'MUNICÍPIO DE TESTE', 'X-' || n, 'SERVIDOR ' || n, 'TRABALHANDO', 'AGENTE', 1000,
                               'servidor ' || n || ' agente x-' || n
                          FROM generate_series(1, :quantidade) n""")
                .param("quantidade", quantidade)
                .update();
    }
}
