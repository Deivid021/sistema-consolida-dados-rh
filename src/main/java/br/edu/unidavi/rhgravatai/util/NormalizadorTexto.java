package br.edu.unidavi.rhgravatai.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza textos para a busca sem diferenciar maiúsculas, minúsculas e acentos (FR-019).
 */
public final class NormalizadorTexto {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private NormalizadorTexto() {
    }

    /** Remove espaços nas pontas e repetidos, acentos e maiúsculas. Texto vazio vira nulo. */
    public static String normalizar(String texto) {
        String limpo = limpar(texto);
        if (limpo == null) {
            return null;
        }
        String semAcento = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(limpo, Normalizer.Form.NFD)).replaceAll("");
        return ESPACOS.matcher(semAcento.toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    /** Remove espaços nas pontas. Texto vazio ou só com espaços vira nulo. */
    public static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        String semEspacos = texto.strip();
        return semEspacos.isEmpty() ? null : semEspacos;
    }
}
