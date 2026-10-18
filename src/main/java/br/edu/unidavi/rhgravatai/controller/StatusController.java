package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.repository.CondicoesVinculo;
import br.edu.unidavi.rhgravatai.service.TetoVigente.Teto;
import br.edu.unidavi.rhgravatai.service.TetoVigente;
import br.edu.unidavi.rhgravatai.service.UltimaAtualizacao;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Situação dos dados publicados: data da última carga, totais e teto em vigor (FR-013). */
@RestController
public class StatusController {

    public static final String FONTE = "API de Dados Abertos de Pessoal - Prefeitura de Gravataí/RS";

    private final JdbcClient jdbc;
    private final UltimaAtualizacao ultimaAtualizacao;
    private final TetoVigente tetoVigente;

    public StatusController(JdbcClient jdbc, UltimaAtualizacao ultimaAtualizacao, TetoVigente tetoVigente) {
        this.jdbc = jdbc;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.tetoVigente = tetoVigente;
    }

    @GetMapping("/api/v1/status")
    public Status status() {
        long total = jdbc.sql("SELECT count(*) FROM vinculo").query(Long.class).single();
        long ativos = jdbc.sql("SELECT count(*) FROM vinculo WHERE " + CondicoesVinculo.ATIVO)
                .query(Long.class).single();
        return new Status(ultimaAtualizacao.obter(), total, ativos, FONTE, tetoVigente.obter());
    }

    public record Status(OffsetDateTime ultimaAtualizacao, long totalVinculos, long totalAtivos, String fonte, Teto teto) {
    }
}
