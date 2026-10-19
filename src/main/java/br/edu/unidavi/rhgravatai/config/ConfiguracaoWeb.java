package br.edu.unidavi.rhgravatai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final String[] origens;

    public ConfiguracaoWeb(@Value("${cors.origens:}") String origens) {
        this.origens = Arrays.stream(origens.split(",")).map(String::strip).filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * Em produção o front-end vai dentro do jar e fica no mesmo endereço da API, sem CORS. As
     * origens configuradas servem para o front-end rodando localmente (Vite, porta 5173).
     */
    @Override
    public void addCorsMappings(CorsRegistry registro) {
        if (origens.length > 0) {
            registro.addMapping("/api/v1/**").allowedOrigins(origens).allowedMethods("GET");
        }
    }

    /**
     * Serve o build do React (classpath:/static). Rotas do front-end como /servidores não são
     * arquivos: respondem com o index.html e o React Router decide a tela. Caminhos da API e
     * arquivos inexistentes (com extensão) continuam dando 404.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String caminho, Resource local) throws IOException {
                        Resource arquivo = local.createRelative(caminho);
                        if (arquivo.exists() && arquivo.isReadable()) {
                            return arquivo;
                        }
                        boolean rotaDoFront = !caminho.startsWith("api/") && !caminho.contains(".");
                        Resource indice = local.createRelative("index.html");
                        return rotaDoFront && indice.exists() ? indice : null;
                    }
                });
    }

    @Bean
    OpenAPI documentacaoApi() {
        return new OpenAPI().info(new Info()
                .title("Dados de RH da Prefeitura de Gravataí/RS")
                .version("1.0.0")
                .description("""
                        Consulta, indicadores, gráficos, alertas e exportação dos dados de pessoal publicados
                        pela API de Dados Abertos de Gravataí, atualizados todo dia às 02:00.

                        **Convenções**
                        - Datas em `yyyy-MM-dd`; valores em reais como número (ex.: `3905.17`).
                        - Campos sem valor na fonte vêm como `null` (exibir "Não informado").
                        - Filtros com vários valores: repita o parâmetro (`regime=CLT&regime=ESTATUTARIO`).
                        - Erros vêm em `application/problem+json`, com a mensagem em `detail`.

                        Contrato de referência: `specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml`."""));
    }
}
