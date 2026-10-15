package br.edu.unidavi.rhgravatai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: PostgreSQL 17 real em contêiner e fonte simulada,
 * compartilhados entre as classes, com todas as tabelas esvaziadas depois de cada teste.
 */
@SpringBootTest(properties = {"admin.token=" + TesteIntegracaoBase.TOKEN_ADMIN, "importacao.cron=-"})
@Import(TesteIntegracaoBase.Configuracao.class)
public abstract class TesteIntegracaoBase {

    protected static final String TOKEN_ADMIN = "token-de-teste";

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected FonteFalsa fonte;

    @Autowired
    private WebApplicationContext contexto;

    protected MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        fonte.usar();
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    /** Depois de cada teste, e não antes, para não apagar os dados carregados por @Sql. */
    @AfterEach
    void limparBanco() {
        jdbc.sql("""
                TRUNCATE alerta, mudanca_vinculo, registro_rejeitado, vinculo_staging,
                         vinculo, parametro_teto, carga RESTART IDENTITY CASCADE""").update();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Configuracao {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:17");
        }

        @Bean
        @Primary
        FonteFalsa fonteFalsa() {
            return new FonteFalsa();
        }
    }
}
