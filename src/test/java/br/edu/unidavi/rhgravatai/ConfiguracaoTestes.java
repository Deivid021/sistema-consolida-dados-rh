package br.edu.unidavi.rhgravatai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL 17 em contêiner e fonte simulada, compartilhados pelos testes de integração. */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracaoTestes {

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
