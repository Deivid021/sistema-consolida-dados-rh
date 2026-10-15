package br.edu.unidavi.rhgravatai.comum;

import java.time.OffsetDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Data e hora do fim da última carga bem-sucedida, exibida em todas as telas (FR-013). */
@Component
public class UltimaAtualizacao {

    private final JdbcClient jdbc;

    public UltimaAtualizacao(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Nulo antes da primeira carga bem-sucedida. */
    public OffsetDateTime obter() {
        return jdbc.sql("SELECT fim FROM carga WHERE status = 'SUCESSO' ORDER BY fim DESC LIMIT 1")
                .query(OffsetDateTime.class)
                .optional()
                .orElse(null);
    }
}
