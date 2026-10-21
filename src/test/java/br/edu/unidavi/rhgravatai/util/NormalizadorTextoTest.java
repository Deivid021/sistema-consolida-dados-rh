package br.edu.unidavi.rhgravatai.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizadorTextoTest {

    @Test
    void removeAcentosMaiusculasEEspacos() {
        assertThat(NormalizadorTexto.normalizar("  JOSÉ  da   Conceição ")).isEqualTo("jose da conceicao");
        assertThat(NormalizadorTexto.normalizar("SEC MUN DA ADMINIST. MODERNIZAÇÃO E TRANSPARÊNCIA"))
                .isEqualTo("sec mun da administ. modernizacao e transparencia");
    }

    @Test
    void textoVazioViraNulo() {
        assertThat(NormalizadorTexto.normalizar(null)).isNull();
        assertThat(NormalizadorTexto.normalizar("   ")).isNull();
        assertThat(NormalizadorTexto.limpar("")).isNull();
        assertThat(NormalizadorTexto.limpar(" CLT ")).isEqualTo("CLT");
    }
}
