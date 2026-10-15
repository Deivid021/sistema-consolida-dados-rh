package br.edu.unidavi.rhgravatai.grafico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

/** História 4: gráficos de movimentação (RF04, FR-030 a FR-032), sobre dados/vinculos-exemplo.sql. */
@Sql("/dados/vinculos-exemplo.sql")
class GraficoIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void movimentacaoMensalIncluiMesesSemMovimento() throws Exception {
        mvc.perform(get("/api/v1/graficos/movimentacao")
                        .param("granularidade", "MES").param("inicio", "2026-01-01").param("fim", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularidade").value("MES"))
                .andExpect(jsonPath("$.periodos[*].periodo").value(contains("2026-01", "2026-02", "2026-03")))
                .andExpect(jsonPath("$.periodos[*].admissoes").value(contains(0, 1, 0)))
                .andExpect(jsonPath("$.periodos[*].desligamentos").value(contains(0, 0, 1)));
    }

    @Test
    void movimentacaoAnual() throws Exception {
        mvc.perform(get("/api/v1/graficos/movimentacao")
                        .param("granularidade", "ANO").param("inicio", "2018-01-01").param("fim", "2020-12-31"))
                .andExpect(jsonPath("$.periodos[*].periodo").value(contains("2018", "2019", "2020")))
                .andExpect(jsonPath("$.periodos[*].admissoes").value(contains(1, 0, 1)))
                .andExpect(jsonPath("$.periodos[*].desligamentos").value(contains(0, 1, 0)));
    }

    @Test
    void movimentacaoRespeitaFiltros() throws Exception {
        mvc.perform(get("/api/v1/graficos/movimentacao").param("granularidade", "ANO")
                        .param("inicio", "2015-01-01").param("fim", "2015-12-31").param("secretaria", "SMS - SAUDE"))
                .andExpect(jsonPath("$.periodos[0].admissoes").value(0));
    }

    @Test
    void motivosAgrupaOutrosENaoInformado() throws Exception {
        for (int i = 1; i <= 11; i++) {
            inserirDesligado("90-%02d".formatted(i), "Motivo %02d".formatted(i), i <= 2 ? 3 : 1);
        }
        inserirDesligado("99-01", null, 1);

        mvc.perform(get("/api/v1/graficos/motivos-rescisao").param("inicio", "1900-01-01").param("fim", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2 + 3 + 3 + 9 + 1))
                .andExpect(jsonPath("$.motivos", hasSize(12)))
                .andExpect(jsonPath("$.motivos[0].motivo").value("Motivo 01"))
                .andExpect(jsonPath("$.motivos[0].quantidade").value(3))
                .andExpect(jsonPath("$.motivos[10].motivo").value("Outros"))
                .andExpect(jsonPath("$.motivos[10].quantidade").value(3))
                .andExpect(jsonPath("$.motivos[11].motivo").value("Não informado"))
                .andExpect(jsonPath("$.motivos[11].quantidade").value(1));
    }

    @Test
    void motivosDoPeriodo() throws Exception {
        mvc.perform(get("/api/v1/graficos/motivos-rescisao").param("inicio", "2026-01-01").param("fim", "2026-12-31"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.motivos[0].motivo").value("Exoneração"));
    }

    @Test
    void parametrosInvalidos() throws Exception {
        mvc.perform(get("/api/v1/graficos/movimentacao")
                        .param("granularidade", "MES").param("inicio", "2026-12-31").param("fim", "2026-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/graficos/movimentacao").param("inicio", "2026-01-01").param("fim", "2026-12-31"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/graficos/movimentacao")
                        .param("granularidade", "SEMANA").param("inicio", "2026-01-01").param("fim", "2026-12-31"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/graficos/motivos-rescisao").param("inicio", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    /** Cria {@code quantidade} desligados com o mesmo motivo, com matrículas derivadas da base. */
    private void inserirDesligado(String matriculaBase, String motivo, int quantidade) {
        for (int n = 0; n < quantidade; n++) {
            jdbc.sql("""
                            INSERT INTO vinculo (entidade, matricula, situacao, data_desligamento, motivo_desligamento)
                            VALUES ('MUNICÍPIO DE TESTE', :matricula, 'DESLIGADO', '2024-05-01', :motivo)""")
                    .param("matricula", matriculaBase + "-" + n)
                    .param("motivo", motivo)
                    .update();
        }
    }
}
