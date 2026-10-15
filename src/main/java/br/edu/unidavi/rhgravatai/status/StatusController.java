package br.edu.unidavi.rhgravatai.status;

import br.edu.unidavi.rhgravatai.importacao.carga.Carga;
import br.edu.unidavi.rhgravatai.importacao.carga.Carga.OrigemTeto;
import br.edu.unidavi.rhgravatai.importacao.carga.CargaRepository;
import br.edu.unidavi.rhgravatai.vinculo.CondicoesVinculo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Situação dos dados publicados: data da última carga, totais e teto em vigor (FR-013). */
@RestController
public class StatusController {

    public static final String FONTE = "API de Dados Abertos de Pessoal - Prefeitura de Gravataí/RS";

    private final CargaRepository cargas;
    private final JdbcClient jdbc;

    public StatusController(CargaRepository cargas, JdbcClient jdbc) {
        this.cargas = cargas;
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/status")
    public Status status() {
        Carga ultima = cargas.ultimaComSucesso().orElse(null);
        long total = jdbc.sql("SELECT count(*) FROM vinculo").query(Long.class).single();
        long ativos = jdbc.sql("SELECT count(*) FROM vinculo WHERE " + CondicoesVinculo.ATIVO)
                .query(Long.class).single();
        Teto teto = ultima == null || ultima.tetoOrigem() == null
                ? new Teto(null, OrigemTeto.SUSPENSA, null)
                : new Teto(ultima.tetoValor(), ultima.tetoOrigem(), fundamento(ultima));
        return new Status(ultima == null ? null : ultima.fim(), total, ativos, FONTE, teto);
    }

    private String fundamento(Carga carga) {
        if (carga.tetoOrigem() != OrigemTeto.MANUAL) {
            return carga.tetoOrigem() == OrigemTeto.FONTE ? "Salário base do cargo PREFEITO publicado pela fonte" : null;
        }
        return jdbc.sql("""
                        SELECT fundamento FROM parametro_teto
                         WHERE vigencia_inicio <= :data ORDER BY vigencia_inicio DESC LIMIT 1""")
                .param("data", carga.inicio().toLocalDate())
                .query(String.class)
                .optional()
                .orElse(null);
    }

    public record Status(OffsetDateTime ultimaAtualizacao, long totalVinculos, long totalAtivos, String fonte, Teto teto) {
    }

    public record Teto(BigDecimal valor, OrigemTeto origem, String fundamento) {
    }
}
