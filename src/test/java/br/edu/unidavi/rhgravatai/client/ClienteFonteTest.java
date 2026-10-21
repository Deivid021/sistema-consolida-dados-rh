package br.edu.unidavi.rhgravatai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.edu.unidavi.rhgravatai.config.PropriedadesFonte;
import br.edu.unidavi.rhgravatai.exception.FonteIndisponivelException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ClienteFonteTest {

    private static final LocalDate INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 10, 15);

    private final List<Duration> esperas = new ArrayList<>();
    private MockRestServiceServer servidor;
    private ClienteFonte cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        var propriedades = new PropriedadesFonte("https://fonte.teste/api/transparencia-pessoal-funcionarios", 1100, 90, 5);
        cliente = new ClienteFonte(builder.build(), propriedades, esperas::add);
    }

    @Test
    void enviaOsParametrosEsperadosPelaFonte() throws IOException {
        servidor.expect(method(HttpMethod.GET))
                .andExpect(queryParam("tipoBusca", "3"))
                .andExpect(queryParam("pagina", "2"))
                .andExpect(queryParam("desligamentoInicio", "01/01/1900"))
                .andExpect(queryParam("desligamentoFinal", "15/10/2026"))
                .andRespond(withSuccess(json("pagina-2.json"), MediaType.APPLICATION_JSON));

        PaginaFonte pagina = cliente.buscarPagina(2, INICIO, FIM);

        assertThat(pagina.totalRegistros()).isEqualTo(5);
        assertThat(pagina.registros()).hasSize(2);
        assertThat(pagina.registros().getFirst().matricula()).isEqualTo("400-01");
        servidor.verify();
    }

    @Test
    void repeteAposLimiteDeRequisicoes() throws IOException {
        servidor.expect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(json("limite-429.json"))
                        .contentType(MediaType.APPLICATION_JSON));
        servidor.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json("pagina-1.json"), MediaType.APPLICATION_JSON));

        PaginaFonte pagina = cliente.buscarPagina(1, INICIO, FIM);

        assertThat(pagina.registros()).hasSize(3);
        assertThat(esperas).contains(Duration.ofSeconds(2));
        servidor.verify();
    }

    @Test
    void erroDisfarcadoEmHttp200ContaComoFalha() throws IOException {
        servidor.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json("erro-parametro.json"), MediaType.APPLICATION_JSON));
        servidor.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json("pagina-1.json"), MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarPagina(1, INICIO, FIM).registros()).hasSize(3);
        servidor.verify();
    }

    @Test
    void desisteDepoisDeCincoTentativas() {
        servidor.expect(times(5), method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> cliente.buscarPagina(1, INICIO, FIM))
                .isInstanceOf(FonteIndisponivelException.class)
                .hasMessageContaining("após 5 tentativas");
        assertThat(esperas).filteredOn(d -> d.toMillis() >= 2000)
                .containsExactly(Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8), Duration.ofSeconds(16));
        servidor.verify();
    }

    @Test
    void limiteDiarioInterrompeSemInsistir() throws IOException {
        servidor.expect(times(1), method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(json("limite-diario.json"))
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.buscarPagina(448, INICIO, FIM))
                .isInstanceOf(FonteIndisponivelException.class)
                .hasMessage("Página 448: limite diário de 1000 requisições da fonte atingido. A carga foi interrompida.");
        assertThat(esperas).filteredOn(d -> d.toMillis() >= 2000).isEmpty();
        servidor.verify();
    }

    @Test
    void esperaOIntervaloMinimoEntreRequisicoes() throws IOException {
        servidor.expect(times(2), method(HttpMethod.GET))
                .andRespond(withSuccess(json("pagina-1.json"), MediaType.APPLICATION_JSON));

        cliente.buscarPagina(1, INICIO, FIM);
        cliente.buscarPagina(2, INICIO, FIM);

        assertThat(esperas).hasSize(1);
        assertThat(esperas.getFirst()).isBetween(Duration.ofMillis(900), Duration.ofMillis(1100));
    }

    @Test
    void cpfNaoExisteNoRegistroLido() {
        assertThat(RegistroFonte.class.getRecordComponents())
                .extracting(c -> c.getName().toLowerCase())
                .doesNotContain("cpf");
    }

    private static String json(String arquivo) throws IOException {
        return new ClassPathResource("fonte/" + arquivo).getContentAsString(StandardCharsets.UTF_8);
    }
}
