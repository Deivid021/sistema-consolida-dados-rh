package br.edu.unidavi.rhgravatai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final String[] origens;

    public ConfiguracaoWeb(@Value("${cors.origens:}") String origens) {
        this.origens = Arrays.stream(origens.split(",")).map(String::strip).filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * Em produção front-end e API ficam no mesmo endereço (CloudFront), sem CORS. As origens
     * configuradas servem para o front-end rodando localmente.
     */
    @Override
    public void addCorsMappings(CorsRegistry registro) {
        if (origens.length > 0) {
            registro.addMapping("/api/v1/**").allowedOrigins(origens).allowedMethods("GET");
        }
    }

    @Bean
    OpenAPI documentacaoApi() {
        return new OpenAPI().info(new Info()
                .title("API de Consulta - Dados de RH de Gravataí/RS")
                .version("1.0.0")
                .description("Contrato de referência: specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml"));
    }
}
