package br.edu.unidavi.rhgravatai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unidavi.rhgravatai.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;

/** Princípio I da constituição e RNF01: nenhuma tabela pode ter coluna de CPF. */
class EsquemaSemCpfTest extends TesteIntegracaoBase {

    @Test
    void nenhumaColunaDeCpf() {
        long colunas = jdbc.sql("""
                        SELECT count(*) FROM information_schema.columns
                         WHERE table_schema = 'public' AND column_name ILIKE '%cpf%'""")
                .query(Long.class).single();

        assertThat(colunas).isZero();
    }
}
