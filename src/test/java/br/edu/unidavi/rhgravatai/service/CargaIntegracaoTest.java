package br.edu.unidavi.rhgravatai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import br.edu.unidavi.rhgravatai.exception.CargaEmAndamentoException;
import br.edu.unidavi.rhgravatai.model.OrigemCarga;
import br.edu.unidavi.rhgravatai.model.StatusCarga;
import br.edu.unidavi.rhgravatai.model.Carga;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** História 1: carga completa contra a fonte simulada (RF01, FR-003, FR-009 a FR-011). */
class CargaIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private CargaService executor;

    @Test
    void cargaCompletaConciliaEPublica() {
        fonte.usar("pagina-1.json", "pagina-2.json");

        Carga carga = executor.executarCompleta(OrigemCarga.MANUAL);

        assertThat(carga.status()).isEqualTo(StatusCarga.SUCESSO);
        assertThat(carga.totalInformado()).isEqualTo(5);
        assertThat(carga.gravados()).isEqualTo(4);
        assertThat(carga.rejeitados()).isEqualTo(1);
        assertThat(carga.paginasLidas()).isEqualTo(2);
        assertThat(carga.novos()).isEqualTo(4);
        assertThat(carga.alterados()).isZero();
        assertThat(carga.fim()).isNotNull();

        assertThat(contar("vinculo")).isEqualTo(4);
        assertThat(contar("vinculo_staging")).isZero();
        assertThat(contar("mudanca_vinculo")).isZero();
        assertThat(jdbc.sql("SELECT campo || ':' || motivo FROM registro_rejeitado").query(String.class).single())
                .isEqualTo("salarioBase:valor inválido (esperado 1.234,56)");
        assertThat(jdbc.sql("SELECT situacao FROM vinculo WHERE matricula = '200-01'").query(String.class).single())
                .isEqualTo("DESLIGADO");
        assertThat(jdbc.sql("SELECT salario_base FROM vinculo WHERE matricula = '400-01'").query(BigDecimal.class).single())
                .isEqualByComparingTo("28936.07");
    }

    @Test
    void totalQueMudaDuranteACargaNaoPublica() {
        fonte.usar("pagina-1.json", "pagina-2.json");
        executor.executarCompleta(OrigemCarga.MANUAL);
        fonte.usar("pagina-1-v2.json", "pagina-2-total-alterado.json");

        Carga carga = executor.executarCompleta(OrigemCarga.MANUAL);

        assertThat(carga.status()).isEqualTo(StatusCarga.INCONSISTENTE);
        assertThat(carga.mensagem()).contains("total da fonte mudou");
        assertDadosDaPrimeiraCargaIntactos();
    }

    @Test
    void falhaDaFonteMantemDadosAnteriores() {
        fonte.usar("pagina-1.json", "pagina-2.json");
        executor.executarCompleta(OrigemCarga.MANUAL);
        fonte.usar("pagina-1-v2.json", "pagina-2-v2.json");
        fonte.falharNaPagina(2);

        Carga carga = executor.executarCompleta(OrigemCarga.MANUAL);

        assertThat(carga.status()).isEqualTo(StatusCarga.FALHA);
        assertThat(carga.mensagem()).contains("Página 2 indisponível");
        assertDadosDaPrimeiraCargaIntactos();
    }

    @Test
    void segundaCargaRegistraMudancasEAusentes() {
        fonte.usar("pagina-1.json", "pagina-2.json");
        executor.executarCompleta(OrigemCarga.MANUAL);
        fonte.usar("pagina-1-v2.json", "pagina-2-v2.json");

        Carga carga = executor.executarCompleta(OrigemCarga.MANUAL);

        assertThat(carga.status()).isEqualTo(StatusCarga.SUCESSO);
        assertThat(carga.novos()).isZero();
        assertThat(carga.alterados()).isEqualTo(2);
        assertThat(carga.naoEncontrados()).isEqualTo(1);

        List<String> mudancas = jdbc.sql("""
                        SELECT v.matricula || ' ' || m.atributo || ' ' || m.valor_anterior || '->' || m.valor_novo
                          FROM mudanca_vinculo m JOIN vinculo v ON v.id = m.vinculo_id
                         ORDER BY v.matricula, m.atributo""")
                .query(String.class).list();
        assertThat(mudancas).containsExactly(
                "100-01 salario_base 3905.17->4100.00",
                "200-01 ativo_na_fonte true->false",
                "300-02 situacao AFASTADO->TRABALHANDO");

        assertThat(jdbc.sql("SELECT ativo_na_fonte FROM vinculo WHERE matricula = '200-01'").query(Boolean.class).single())
                .isFalse();
        assertThat(contar("vinculo")).isEqualTo(4);
    }

    @Test
    void nenhumCpfGravadoEmLugarNenhum() {
        fonte.usar("pagina-1.json", "pagina-2.json");
        executor.executarCompleta(OrigemCarga.MANUAL);

        List<String[]> colunas = jdbc.sql("""
                        SELECT table_name, column_name FROM information_schema.columns
                         WHERE table_schema = 'public' AND data_type IN ('text', 'jsonb', 'character varying')""")
                .query((rs, i) -> new String[] {rs.getString(1), rs.getString(2)})
                .list();
        assertThat(colunas).isNotEmpty();
        for (String[] c : colunas) {
            long ocorrencias = jdbc.sql("SELECT count(*) FROM " + c[0] + " WHERE " + c[1]
                            + "::text ~ '\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\*\\*\\*\\.'")
                    .query(Long.class).single();
            assertThat(ocorrencias).as("CPF em %s.%s", c[0], c[1]).isZero();
        }
    }

    @Test
    void umaCargaPorVez() throws Exception {
        executor.iniciar(OrigemCarga.MANUAL);

        assertThatThrownBy(() -> executor.iniciar(OrigemCarga.AGENDADA)).isInstanceOf(CargaEmAndamentoException.class);
        mvc.perform(post("/api/v1/admin/cargas").header("X-Admin-Token", TOKEN_ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma carga em andamento. Aguarde ela terminar."));
    }

    @Test
    void administracaoExigeToken() throws Exception {
        mvc.perform(post("/api/v1/admin/cargas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/cargas").header("X-Admin-Token", "errado")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/cargas").header("X-Admin-Token", TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void statusMostraUltimaCargaETotais() throws Exception {
        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimaAtualizacao").doesNotExist())
                .andExpect(jsonPath("$.teto.origem").value("SUSPENSA"));
        mvc.perform(get("/api/v1/vinculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.ultimaAtualizacao").doesNotExist());

        fonte.usar("pagina-1.json", "pagina-2.json");
        executor.executarCompleta(OrigemCarga.MANUAL);

        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimaAtualizacao").isNotEmpty())
                .andExpect(jsonPath("$.totalVinculos").value(4))
                .andExpect(jsonPath("$.totalAtivos").value(3))
                .andExpect(jsonPath("$.teto.origem").value("FONTE"))
                .andExpect(jsonPath("$.teto.valor").value(28936.07))
                .andExpect(jsonPath("$.fonte").value("API de Dados Abertos de Pessoal - Prefeitura de Gravataí/RS"));
    }

    private void assertDadosDaPrimeiraCargaIntactos() {
        assertThat(contar("vinculo")).isEqualTo(4);
        assertThat(contar("mudanca_vinculo")).isZero();
        assertThat(contar("vinculo_staging")).isZero();
        assertThat(jdbc.sql("SELECT salario_base FROM vinculo WHERE matricula = '100-01'").query(BigDecimal.class).single())
                .isEqualByComparingTo("3905.17");
        assertThat(jdbc.sql("SELECT count(*) FROM vinculo WHERE NOT ativo_na_fonte").query(Long.class).single()).isZero();
    }

    private long contar(String tabela) {
        return jdbc.sql("SELECT count(*) FROM " + tabela).query(Long.class).single();
    }
}
