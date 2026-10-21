package br.edu.unidavi.rhgravatai.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.unidavi.rhgravatai.client.RegistroFonte;
import br.edu.unidavi.rhgravatai.model.RegistroTratado;
import br.edu.unidavi.rhgravatai.model.RegistroRejeitado;
import br.edu.unidavi.rhgravatai.model.Situacao;
import br.edu.unidavi.rhgravatai.service.TratamentoRegistroService.Resultado;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TratamentoRegistroServiceTest {

    private final TratamentoRegistroService tratador = new TratamentoRegistroService();

    @Test
    void converteValoresDatasEHoras() {
        RegistroTratado r = valido(registro("100-01", "Trabalhando", "29/01/1979", null, "3.905,17", "180:00", "036:00"));

        assertThat(r.matricula()).isEqualTo("100-01");
        assertThat(r.situacao()).isEqualTo(Situacao.TRABALHANDO);
        assertThat(r.dataAdmissao()).isEqualTo(LocalDate.of(1979, 1, 29));
        assertThat(r.salarioBase()).isEqualByComparingTo(new BigDecimal("3905.17"));
        assertThat(r.horasMensaisMin()).isEqualTo(10800);
        assertThat(r.horasSemanaisMin()).isEqualTo(2160);
        assertThat(r.valoresOriginais()).containsEntry("salarioBase", "3.905,17")
                .containsEntry("admissao", "29/01/1979")
                .containsEntry("horasMensais", "180:00");
        assertThat(r.textoBusca()).isEqualTo("jose da silva programador 100-01");
    }

    @Test
    void salarioComMilharesEZero() {
        assertThat(valido(registro("1-01", "Trabalhando", null, null, "28.936,07", null, null)).salarioBase())
                .isEqualByComparingTo("28936.07");
        assertThat(valido(registro("1-01", "Trabalhando", null, null, "0,00", null, null)).salarioBase())
                .isEqualByComparingTo("0");
    }

    @Test
    void demitidoViraDesligadoComDataDeRescisao() {
        RegistroTratado r = valido(registro("27-01", "Demitido", "05/08/1974", "01/06/2026", "3.905,17", null, null));

        assertThat(r.situacao()).isEqualTo(Situacao.DESLIGADO);
        assertThat(r.dataDesligamento()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void textosVaziosViramNulo() {
        RegistroFonte fonte = new RegistroFonte("MUNICÍPIO DE TESTE", " 1-01 ", "  ", "Afastado", null, null, null,
                " ", "", null, "SMS", null, "  CARGO  ", null, "   ", null, null, null, null);

        RegistroTratado r = valido(fonte);

        assertThat(r.matricula()).isEqualTo("1-01");
        assertThat(r.nome()).isNull();
        assertThat(r.regime()).isNull();
        assertThat(r.classificacao()).isNull();
        assertThat(r.nivel()).isNull();
        assertThat(r.cargo()).isEqualTo("CARGO");
        assertThat(r.valoresOriginais()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULO", textBlock = """
            NULO  | Trabalhando | 01/01/2020 | 1.000,00 | 180:00 | matricula     | matrícula ausente
            1-01  | Aposentado  | 01/01/2020 | 1.000,00 | 180:00 | situacao      | situação desconhecida
            1-01  | Trabalhando | 31/02/2026 | 1.000,00 | 180:00 | admissao      | data inválida (esperado dd/mm/aaaa)
            1-01  | Trabalhando | 2020-01-01 | 1.000,00 | 180:00 | admissao      | data inválida (esperado dd/mm/aaaa)
            1-01  | Trabalhando | 01/01/2020 | abc      | 180:00 | salarioBase   | valor inválido (esperado 1.234,56)
            1-01  | Trabalhando | 01/01/2020 | 1000.00  | 180:00 | salarioBase   | valor inválido (esperado 1.234,56)
            1-01  | Trabalhando | 01/01/2020 | 1.000,00 | 18h    | horasMensais  | horas inválidas (esperado HHH:MM)
            1-01  | Trabalhando | 01/01/2020 | 1.000,00 | 180:75 | horasMensais  | horas inválidas (esperado HHH:MM)
            """)
    void rejeitaComCampoEMotivo(String matricula, String situacao, String admissao, String salario, String horas,
                                String campo, String motivo) {
        Resultado resultado = tratador.tratar(registro(matricula, situacao, admissao, null, salario, horas, null));

        assertThat(resultado).isInstanceOf(Resultado.Rejeitado.class);
        RegistroRejeitado rejeicao = ((Resultado.Rejeitado) resultado).rejeicao();
        assertThat(rejeicao.campo()).isEqualTo(campo);
        assertThat(rejeicao.motivo()).isEqualTo(motivo);
        assertThat(rejeicao.matricula()).isEqualTo(matricula);
    }

    private RegistroTratado valido(RegistroFonte fonte) {
        Resultado resultado = tratador.tratar(fonte);
        assertThat(resultado).isInstanceOf(Resultado.Valido.class);
        return ((Resultado.Valido) resultado).registro();
    }

    private static RegistroFonte registro(String matricula, String situacao, String admissao, String rescisao,
                                          String salario, String horasMensais, String horasSemanais) {
        return new RegistroFonte("MUNICÍPIO DE TESTE", matricula, "JOSÉ DA SILVA", situacao, admissao, rescisao,
                rescisao == null ? null : "Exoneração", "CLT", null, null, "SMS - SAUDE", "UBS CENTRO",
                "PROGRAMADOR", null, "218", salario, "180h", horasMensais, horasSemanais);
    }
}
