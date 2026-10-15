package br.edu.unidavi.rhgravatai.vinculo;

import java.util.Optional;

/**
 * Situação do vínculo. A fonte usa "Demitido" para qualquer desligamento (aposentadoria,
 * exoneração, falecimento, fim de contrato), por isso o sistema chama de DESLIGADO (FR-023).
 */
public enum Situacao {
    TRABALHANDO,
    AFASTADO,
    DESLIGADO;

    /** Converte o texto da fonte ("Trabalhando", "Afastado", "Demitido"). */
    public static Optional<Situacao> daFonte(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        return switch (texto.strip().toLowerCase()) {
            case "trabalhando" -> Optional.of(TRABALHANDO);
            case "afastado" -> Optional.of(AFASTADO);
            case "demitido" -> Optional.of(DESLIGADO);
            default -> Optional.empty();
        };
    }
}
