package br.edu.unidavi.rhgravatai.importacao.fonte;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração do acesso à API de Gravataí (prefixo {@code fonte} no application.yml). */
@ConfigurationProperties("fonte")
public record PropriedadesFonte(
        String urlBase,
        int intervaloMinimoMs,
        int timeoutS,
        int maxTentativas) {
}
