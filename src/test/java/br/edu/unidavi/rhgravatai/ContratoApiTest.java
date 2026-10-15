package br.edu.unidavi.rhgravatai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Todo caminho e método do contrato com o front-end deve existir na API publicada
 * (specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml). Divergência é defeito.
 */
class ContratoApiTest extends TesteIntegracaoBase {

    private static final Path CONTRATO = Path.of("specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml");

    @Test
    void todasAsOperacoesDoContratoExistem() throws Exception {
        Set<String> esperadas = new TreeSet<>();
        try (Reader leitor = Files.newBufferedReader(CONTRATO, StandardCharsets.UTF_8)) {
            Map<String, Object> contrato = new Yaml().load(leitor);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> caminhos = (Map<String, Map<String, Object>>) contrato.get("paths");
            caminhos.forEach((caminho, metodos) -> metodos.keySet().forEach(metodo ->
                    esperadas.add(metodo.toUpperCase(Locale.ROOT) + " /api/v1" + caminho)));
        }

        String documentacao = mvc.perform(get("/v3/api-docs")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode publicada = JsonMapper.builder().build().readTree(documentacao).get("paths");
        Set<String> existentes = new TreeSet<>();
        publicada.propertyNames().forEach(caminho -> publicada.get(caminho).propertyNames()
                .forEach(metodo -> existentes.add(metodo.toUpperCase(Locale.ROOT) + " " + caminho)));

        assertThat(esperadas).hasSize(11);
        assertThat(existentes).containsAll(esperadas);
    }
}
