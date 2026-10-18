package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.model.Carga.OrigemTeto;
import br.edu.unidavi.rhgravatai.repository.CondicoesVinculo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Define o teto municipal de uma carga (FR-037):
 * <ol>
 *   <li>valor manual em {@code parametro_teto} com a maior vigência não posterior à data;</li>
 *   <li>senão, salário base do vínculo ativo de cargo PREFEITO publicado pela fonte;</li>
 *   <li>senão, a regra fica suspensa.</li>
 * </ol>
 * O vínculo do prefeito é sempre identificado, porque nunca deve ser sinalizado.
 */
@Component
public class ResolvedorTeto {

    public static final String CARGO_PREFEITO = "PREFEITO";

    private final JdbcClient jdbc;

    public ResolvedorTeto(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public TetoResolvido resolver(LocalDate data) {
        Optional<Prefeito> prefeito = jdbc.sql("""
                        SELECT id, salario_base FROM vinculo
                         WHERE cargo = :cargo AND salario_base > 0 AND %s
                         ORDER BY data_admissao DESC NULLS LAST, id DESC
                         LIMIT 1""".formatted(CondicoesVinculo.ATIVO))
                .param("cargo", CARGO_PREFEITO)
                .query((rs, i) -> new Prefeito(rs.getLong("id"), rs.getBigDecimal("salario_base")))
                .optional();
        Long prefeitoId = prefeito.map(Prefeito::id).orElse(null);

        Optional<TetoResolvido> manual = jdbc.sql("""
                        SELECT valor, fundamento FROM parametro_teto
                         WHERE vigencia_inicio <= :data
                         ORDER BY vigencia_inicio DESC
                         LIMIT 1""")
                .param("data", data)
                .query((rs, i) -> new TetoResolvido(rs.getBigDecimal("valor"), OrigemTeto.MANUAL,
                        rs.getString("fundamento"), prefeitoId))
                .optional();
        if (manual.isPresent()) {
            return manual.get();
        }
        return prefeito
                .map(p -> new TetoResolvido(p.salario(), OrigemTeto.FONTE,
                        "Salário base do cargo PREFEITO publicado pela fonte", p.id()))
                .orElse(new TetoResolvido(null, OrigemTeto.SUSPENSA, null, null));
    }

    /**
     * @param valor nulo quando a regra está suspensa
     * @param prefeitoId vínculo do prefeito, que nunca é sinalizado; nulo se não houver
     */
    public record TetoResolvido(BigDecimal valor, OrigemTeto origem, String fundamento, Long prefeitoId) {
    }

    private record Prefeito(long id, BigDecimal salario) {
    }
}
