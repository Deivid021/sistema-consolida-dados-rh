package br.edu.unidavi.rhgravatai.importacao.fonte;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente da API de Dados Abertos de Pessoal de Gravataí (RF01, FR-004).
 *
 * <p>Comportamento da fonte (research.md §1): limite de 1 requisição por segundo, com HTTP 429
 * no excesso; erros de parâmetro chegam com HTTP 200 e sem {@code registros}; páginas podem
 * levar até 17 s. Por isso: intervalo mínimo entre requisições, novas tentativas com espera
 * crescente e falha só depois de esgotar as tentativas.
 *
 * <p>O corpo das respostas nunca é registrado em log, porque contém dados pessoais.
 */
@Component
public class ClienteFonte implements FontePessoal {

    private static final Logger log = LoggerFactory.getLogger(ClienteFonte.class);
    private static final DateTimeFormatter DATA_FONTE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient rest;
    private final PropriedadesFonte propriedades;
    private final Esperador esperador;
    private long ultimaRequisicaoNanos;

    @Autowired
    public ClienteFonte(PropriedadesFonte propriedades, Esperador esperador) {
        this(criarRestClient(propriedades), propriedades, esperador);
    }

    ClienteFonte(RestClient rest, PropriedadesFonte propriedades, Esperador esperador) {
        this.rest = rest;
        this.propriedades = propriedades;
        this.esperador = esperador;
    }

    @Override
    public synchronized PaginaFonte buscarPagina(int pagina, LocalDate desligamentoInicio, LocalDate desligamentoFim) {
        // A URI é montada à mão para manter as barras das datas sem codificação, como a fonte espera.
        URI uri = URI.create(propriedades.urlBase()
                + "?tipoBusca=3"
                + "&pagina=" + pagina
                + "&desligamentoInicio=" + DATA_FONTE.format(desligamentoInicio)
                + "&desligamentoFinal=" + DATA_FONTE.format(desligamentoFim));

        String ultimoErro = null;
        for (int tentativa = 1; tentativa <= propriedades.maxTentativas(); tentativa++) {
            respeitarIntervalo();
            try {
                Tentativa resultado = rest.get().uri(uri).exchange((requisicao, resposta) -> {
                    int status = resposta.getStatusCode().value();
                    if (resposta.getStatusCode().isError()) {
                        return Tentativa.falha("HTTP " + status);
                    }
                    PaginaFonte corpo = resposta.bodyTo(PaginaFonte.class);
                    if (corpo == null || corpo.registros() == null) {
                        return Tentativa.falha("resposta sem registros (HTTP " + status + ")");
                    }
                    return Tentativa.sucesso(corpo);
                });
                if (resultado.pagina() != null) {
                    return resultado.pagina();
                }
                ultimoErro = resultado.erro();
            } catch (RestClientException e) {
                ultimoErro = e.getClass().getSimpleName();
            }
            log.warn("Fonte: página {} falhou na tentativa {}/{}: {}",
                    pagina, tentativa, propriedades.maxTentativas(), ultimoErro);
            if (tentativa < propriedades.maxTentativas()) {
                esperador.esperar(Duration.ofSeconds(1L << tentativa));
            }
        }
        throw new FonteIndisponivelException("Página " + pagina + " indisponível após "
                + propriedades.maxTentativas() + " tentativas (" + ultimoErro + ").");
    }

    private void respeitarIntervalo() {
        long intervalo = Duration.ofMillis(propriedades.intervaloMinimoMs()).toNanos();
        long decorrido = System.nanoTime() - ultimaRequisicaoNanos;
        if (ultimaRequisicaoNanos != 0 && decorrido < intervalo) {
            esperador.esperar(Duration.ofNanos(intervalo - decorrido));
        }
        ultimaRequisicaoNanos = System.nanoTime();
    }

    private static RestClient criarRestClient(PropriedadesFonte propriedades) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(propriedades.timeoutS()));
        return RestClient.builder()
                .requestFactory(fabrica)
                .defaultHeader("User-Agent", "rh-gravatai/0.1 (projeto academico Unidavi)")
                .build();
    }

    private record Tentativa(PaginaFonte pagina, String erro) {

        static Tentativa sucesso(PaginaFonte pagina) {
            return new Tentativa(pagina, null);
        }

        static Tentativa falha(String erro) {
            return new Tentativa(null, erro);
        }
    }
}
