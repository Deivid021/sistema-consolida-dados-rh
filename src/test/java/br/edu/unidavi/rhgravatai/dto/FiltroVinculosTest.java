package br.edu.unidavi.rhgravatai.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.model.Situacao;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FiltroVinculosTest {

    @Test
    void semFiltrosNaoRestringe() {
        assertThat(FiltroVinculos.vazio().paraSql("").where()).isEqualTo("TRUE");
    }

    @Test
    void combinaFiltrosComEeValoresComOu() {
        var filtro = new FiltroVinculos(null, List.of(Situacao.AFASTADO, Situacao.TRABALHANDO),
                List.of("CLT", "ESTATUTARIO"), null, List.of("SMS"), null, null, null, null, null, null);

        var sql = filtro.paraSql("v.");

        assertThat(sql.where()).isEqualTo(
                "v.situacao IN (:fsituacao) AND (v.regime IN (:fregime)) AND (v.secretaria IN (:fsecretaria))");
        assertThat(sql.parametros()).containsEntry("fsituacao", List.of("AFASTADO", "TRABALHANDO"))
                .containsEntry("fregime", List.of("CLT", "ESTATUTARIO"));
    }

    @Test
    void naoInformadoViraIsNull() {
        var soNaoInformado = new FiltroVinculos(null, null, null, List.of("NAO_INFORMADO"), null, null, null,
                null, null, null, null);
        var misturado = new FiltroVinculos(null, null, null, List.of("PENSIONISTA", "NAO_INFORMADO"), null, null,
                null, null, null, null, null);

        assertThat(soNaoInformado.paraSql("").where()).isEqualTo("(classificacao IS NULL)");
        assertThat(misturado.paraSql("").where()).isEqualTo("(classificacao IN (:fclassificacao) OR classificacao IS NULL)");
    }

    @Test
    void buscaNormalizadaEComCuringasEscapados() {
        var filtro = new FiltroVinculos("  José_100% ", null, null, null, null, null, null, null, null, null, null);

        var sql = filtro.paraSql("");

        assertThat(sql.where()).isEqualTo("texto_busca LIKE :fq");
        assertThat(sql.parametros()).containsEntry("fq", "%jose\\_100\\%%");
    }

    @Test
    void buscaCurtaEInvalida() {
        var filtro = new FiltroVinculos("a", null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> filtro.paraSql("")).isInstanceOf(FiltroInvalidoException.class)
                .hasMessageContaining("pelo menos 2 caracteres");
    }

    @Test
    void periodoIncluiAsDatasEAceitaSoUma() {
        var filtro = new FiltroVinculos(null, null, null, null, null, null, null,
                LocalDate.of(2026, 1, 1), null, null, null);

        assertThat(filtro.paraSql("").where()).isEqualTo("data_admissao >= :fadmInicio");
    }

    @Test
    void periodoInvertidoEInvalido() {
        var filtro = new FiltroVinculos(null, null, null, null, null, null, null,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), null, null);

        assertThatThrownBy(() -> filtro.paraSql("")).isInstanceOf(FiltroInvalidoException.class)
                .hasMessageContaining("data inicial não pode ser maior");
    }

    @Test
    void periodoDeDesligamentoRetornaSoDesligados() {
        var filtro = new FiltroVinculos(null, null, null, null, null, null, null, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(filtro.paraSql("").where()).isEqualTo(
                "situacao = 'DESLIGADO' AND data_desligamento >= :fdesInicio AND data_desligamento <= :fdesFim");
    }
}
