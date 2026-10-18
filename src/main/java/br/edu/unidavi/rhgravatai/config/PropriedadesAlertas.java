package br.edu.unidavi.rhgravatai.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parâmetros da regra de salário fora do padrão (prefixo {@code alertas} no application.yml). */
@ConfigurationProperties("alertas")
public record PropriedadesAlertas(ForaPadrao foraPadrao) {

    /**
     * @param percentual diferença em relação à mediana do grupo acima da qual há alerta (FR-038)
     * @param minimoGrupo tamanho mínimo do grupo para a regra valer (FR-039)
     */
    public record ForaPadrao(BigDecimal percentual, int minimoGrupo) {
    }
}
