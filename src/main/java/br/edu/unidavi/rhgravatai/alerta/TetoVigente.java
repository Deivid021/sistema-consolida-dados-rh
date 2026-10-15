package br.edu.unidavi.rhgravatai.alerta;

import br.edu.unidavi.rhgravatai.importacao.carga.Carga;
import br.edu.unidavi.rhgravatai.importacao.carga.Carga.OrigemTeto;
import br.edu.unidavi.rhgravatai.importacao.carga.CargaRepository;
import java.math.BigDecimal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Teto usado na última carga publicada, exibido em /status e /alertas. */
@Component
public class TetoVigente {

    private final CargaRepository cargas;
    private final JdbcClient jdbc;

    public TetoVigente(CargaRepository cargas, JdbcClient jdbc) {
        this.cargas = cargas;
        this.jdbc = jdbc;
    }

    public Teto obter() {
        Carga ultima = cargas.ultimaComSucesso().orElse(null);
        if (ultima == null || ultima.tetoOrigem() == null || ultima.tetoOrigem() == OrigemTeto.SUSPENSA) {
            return new Teto(null, OrigemTeto.SUSPENSA, null);
        }
        return new Teto(ultima.tetoValor(), ultima.tetoOrigem(), fundamento(ultima));
    }

    private String fundamento(Carga carga) {
        if (carga.tetoOrigem() == OrigemTeto.FONTE) {
            return "Salário base do cargo PREFEITO publicado pela fonte";
        }
        return jdbc.sql("""
                        SELECT fundamento FROM parametro_teto
                         WHERE vigencia_inicio <= :data ORDER BY vigencia_inicio DESC LIMIT 1""")
                .param("data", carga.dataInicio())
                .query(String.class)
                .optional()
                .orElse(null);
    }

    /** Valor nulo quando a regra está suspensa. */
    public record Teto(BigDecimal valor, OrigemTeto origem, String fundamento) {
    }
}
