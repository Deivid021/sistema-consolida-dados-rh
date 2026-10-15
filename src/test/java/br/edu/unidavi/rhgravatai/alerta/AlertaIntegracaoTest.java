package br.edu.unidavi.rhgravatai.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * História 5: regras de alerta com casos de fronteira (RF06, FR-037 a FR-042, SC-007).
 * Revisão sugerida pelos responsáveis por qualidade (T046, T047).
 */
class AlertaIntegracaoTest extends TesteIntegracaoBase {

    private static final int HORAS_200 = 12000;

    @Autowired
    private CalculadorAlertas calculador;

    private long cargaId;

    @BeforeEach
    void criarCarga() {
        cargaId = jdbc.sql("""
                        INSERT INTO carga (origem, status, inicio, fim, desligamento_inicio, desligamento_fim)
                        VALUES ('MANUAL', 'SUCESSO', '2026-10-15 02:00:00-03', '2026-10-15 02:15:00-03',
                                '1900-01-01', '2026-10-15')
                        RETURNING id""")
                .query(Long.class).single();
    }

    // ---- Salário fora do padrão (FR-038, FR-039) ----

    @Test
    void diferencaDeExatamente20PorCentoNaoGeraAlerta() {
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "1200", "800");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).isEmpty();
    }

    @Test
    void diferencaAcimaDe20PorCentoGeraAlertaParaCimaEParaBaixo() {
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "1200.01", "799.99");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).containsExactlyInAnyOrder("AGENTE-4 1200.01 ref 1000.00", "AGENTE-5 799.99 ref 1000.00");
    }

    @Test
    void grupoComMenosDeCincoVinculosNaoGeraAlerta() {
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "5000");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).isEmpty();
    }

    @Test
    void nivelAusenteESalarioZeroFicamDeFora() {
        grupo("SEM NIVEL", null, HORAS_200, "1000", "1000", "1000", "1000", "9000");
        grupo("COM ZERO", "1", HORAS_200, "1000", "1000", "1000", "1000", "0");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).isEmpty();
    }

    @Test
    void grupoParUsaAMediaDosDoisValoresCentrais() {
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "1001", "3000", "3000");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).containsExactlyInAnyOrder(
                "AGENTE-5 3000.00 ref 1000.50", "AGENTE-6 3000.00 ref 1000.50");
    }

    @Test
    void cargaHorariaDiferenteFormaOutroGrupo() {
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "1000", "1000");
        grupo("AGENTE", "1", 6000, "500", "500", "500", "500", "500");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).isEmpty();
    }

    @Test
    void inativosNaoEntramNoGrupoNemSaoSinalizados() {
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "1000", "1000");
        vinculo("DESLIGADO-1", "AGENTE", "1", HORAS_200, "9000", "DESLIGADO", true);
        vinculo("AUSENTE-1", "AGENTE", "1", HORAS_200, "9000", "TRABALHANDO", false);

        calculador.aposPublicar(cargaId);

        assertThat(alertas("FORA_PADRAO")).isEmpty();
    }

    // ---- Teto municipal (FR-037) ----

    @Test
    void semPrefeitoNemValorManualARegraFicaSuspensa() {
        vinculo("ALTO-1", "PROCURADOR", "1", HORAS_200, "50000", "TRABALHANDO", true);

        calculador.aposPublicar(cargaId);

        assertThat(alertas("TETO")).isEmpty();
        assertThat(tetoDaCarga()).isEqualTo("SUSPENSA null");
    }

    @Test
    void tetoVemDoPrefeitoEIgualNaoESinalizado() {
        vinculo("PREF-1", "PREFEITO", "388", 13200, "28936.07", "TRABALHANDO", true);
        vinculo("IGUAL-1", "PROCURADOR", "1", HORAS_200, "28936.07", "TRABALHANDO", true);
        vinculo("ACIMA-1", "PROCURADOR", "1", HORAS_200, "30000", "AFASTADO", true);
        vinculo("DESLIG-1", "PROCURADOR", "1", HORAS_200, "40000", "DESLIGADO", true);

        calculador.aposPublicar(cargaId);

        assertThat(alertas("TETO")).containsExactly("ACIMA-1 30000.00 ref 28936.07");
        assertThat(tetoDaCarga()).isEqualTo("FONTE 28936.07");
    }

    @Test
    void valorManualVigentePrevaleceEPrefeitoNuncaESinalizado() {
        vinculo("PREF-1", "PREFEITO", "388", 13200, "28936.07", "TRABALHANDO", true);
        vinculo("ACIMA-1", "PROCURADOR", "1", HORAS_200, "26000", "TRABALHANDO", true);
        parametroTeto("20000", "2025-01-01", "Lei antiga");
        parametroTeto("25000", "2026-01-01", "Lei vigente");
        parametroTeto("10000", "2099-01-01", "Lei futura");

        calculador.aposPublicar(cargaId);

        assertThat(alertas("TETO")).containsExactly("ACIMA-1 26000.00 ref 25000.00");
        assertThat(tetoDaCarga()).isEqualTo("MANUAL 25000.00");
    }

    @Test
    void recalculoSubstituiOsAlertasAnteriores() {
        vinculo("PREF-1", "PREFEITO", "388", 13200, "28936.07", "TRABALHANDO", true);
        vinculo("ACIMA-1", "PROCURADOR", "1", HORAS_200, "30000", "TRABALHANDO", true);
        calculador.aposPublicar(cargaId);
        jdbc.sql("UPDATE vinculo SET salario_base = 20000 WHERE matricula = 'ACIMA-1'").update();

        calculador.aposPublicar(cargaId);

        assertThat(alertas("TETO")).isEmpty();
    }

    // ---- Listagem (FR-040 a FR-042) ----

    @Test
    void listagemMostraCriterioTetoEPercentual() throws Exception {
        vinculo("PREF-1", "PREFEITO", "388", 13200, "28936.07", "TRABALHANDO", true);
        vinculo("ACIMA-1", "PROCURADOR", "1", HORAS_200, "30000", "TRABALHANDO", true);
        grupo("AGENTE", "1", HORAS_200, "1000", "1000", "1000", "1000", "1250");
        calculador.aposPublicar(cargaId);

        mvc.perform(get("/api/v1/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.teto.origem").value("FONTE"))
                .andExpect(jsonPath("$.teto.valor").value(28936.07))
                .andExpect(jsonPath("$.percentualForaPadrao").value(20))
                .andExpect(jsonPath("$.itens[0].regra").value("FORA_PADRAO"))
                .andExpect(jsonPath("$.itens[0].vinculo.matricula").value("AGENTE-5"))
                .andExpect(jsonPath("$.itens[0].grupoTamanho").value(5))
                .andExpect(jsonPath("$.itens[0].descricaoRegra").value(
                        "Salário base 25,0% acima da mediana (R$ 1.000,00) dos 5 vínculos ativos com o mesmo cargo, nível e carga horária."))
                .andExpect(jsonPath("$.itens[1].regra").value("TETO"))
                .andExpect(jsonPath("$.itens[1].diferenca").value(1063.93))
                .andExpect(jsonPath("$.itens[1].descricaoRegra").value(
                        "Salário base R$ 1.063,93 acima do teto municipal (subsídio do prefeito) de R$ 28.936,07."));

        mvc.perform(get("/api/v1/alertas").param("regra", "TETO"))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/alertas").param("cargo", "AGENTE"))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/alertas").param("regra", "OUTRA"))
                .andExpect(status().isBadRequest());
    }

    private void grupo(String cargo, String nivel, int horasMensaisMin, String... salarios) {
        for (int i = 0; i < salarios.length; i++) {
            String sufixo = nivel == null ? "SN" : String.valueOf(horasMensaisMin);
            String matricula = horasMensaisMin == HORAS_200 && nivel != null ? cargo + "-" + (i + 1)
                    : cargo + "-" + sufixo + "-" + (i + 1);
            vinculo(matricula, cargo, nivel, horasMensaisMin, salarios[i], "TRABALHANDO", true);
        }
    }

    private void vinculo(String matricula, String cargo, String nivel, int horasMensaisMin, String salario,
                         String situacao, boolean ativoNaFonte) {
        jdbc.sql("""
                        INSERT INTO vinculo (entidade, matricula, nome, situacao, cargo, nivel, salario_base,
                                             horas_mensais_min, ativo_na_fonte)
                        VALUES ('MUNICÍPIO DE TESTE', :matricula, :matricula, :situacao, :cargo, :nivel, :salario,
                                :horas, :ativo)""")
                .param("matricula", matricula)
                .param("situacao", situacao)
                .param("cargo", cargo)
                .param("nivel", nivel)
                .param("salario", new BigDecimal(salario))
                .param("horas", horasMensaisMin)
                .param("ativo", ativoNaFonte)
                .update();
    }

    private void parametroTeto(String valor, String vigencia, String fundamento) {
        jdbc.sql("INSERT INTO parametro_teto (valor, vigencia_inicio, fundamento) VALUES (:valor, CAST(:vigencia AS date), :fundamento)")
                .param("valor", new BigDecimal(valor))
                .param("vigencia", vigencia)
                .param("fundamento", fundamento)
                .update();
    }

    private List<String> alertas(String regra) {
        return jdbc.sql("""
                        SELECT v.matricula || ' ' || a.valor_vinculo || ' ref ' || a.valor_referencia
                          FROM alerta a JOIN vinculo v ON v.id = a.vinculo_id
                         WHERE a.regra = :regra ORDER BY v.matricula""")
                .param("regra", regra)
                .query(String.class)
                .list();
    }

    private String tetoDaCarga() {
        return jdbc.sql("SELECT teto_origem || ' ' || coalesce(teto_valor::text, 'null') FROM carga WHERE id = :id")
                .param("id", cargaId)
                .query(String.class)
                .single();
    }
}
