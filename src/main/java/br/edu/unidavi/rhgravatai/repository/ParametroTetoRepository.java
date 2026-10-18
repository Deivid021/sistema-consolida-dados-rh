package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.model.ParametroTeto;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Valores manuais do teto municipal, mantidos pela equipe por SQL (FR-037). */
@Repository
public class ParametroTetoRepository {

    private final JdbcClient jdbc;

    public ParametroTetoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** O valor com a maior vigência que não seja posterior à data. */
    public Optional<ParametroTeto> vigenteEm(LocalDate data) {
        return jdbc.sql("""
                        SELECT valor, vigencia_inicio, fundamento FROM parametro_teto
                         WHERE vigencia_inicio <= :data
                         ORDER BY vigencia_inicio DESC
                         LIMIT 1""")
                .param("data", data)
                .query((rs, i) -> new ParametroTeto(rs.getBigDecimal("valor"),
                        rs.getObject("vigencia_inicio", LocalDate.class), rs.getString("fundamento")))
                .optional();
    }
}
