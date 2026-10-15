package br.edu.unidavi.rhgravatai.exportacao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Conteúdo de um relatório exportado, independente do formato (FR-033 a FR-035).
 *
 * @param linhas fluxo das linhas; deve ser fechado depois do uso
 */
public record Relatorio<T>(
        String titulo,
        String nomeArquivo,
        List<String> filtros,
        OffsetDateTime ultimaAtualizacao,
        List<Coluna<T>> colunas,
        Stream<T> linhas,
        String rotuloTotal,
        long total) {

    /**
     * @param valor valor da célula: texto, número, {@link java.math.BigDecimal} (formatado como
     *              moeda se {@code moeda}), {@link java.time.LocalDate} ou nulo
     * @param noPdf se a coluna aparece no PDF; o CSV traz sempre todas
     * @param largura largura relativa no PDF
     */
    public record Coluna<T>(String titulo, Function<T, Object> valor, boolean moeda, boolean noPdf, float largura) {

        public static <T> Coluna<T> texto(String titulo, Function<T, Object> valor, float largura) {
            return new Coluna<>(titulo, valor, false, true, largura);
        }

        public static <T> Coluna<T> moeda(String titulo, Function<T, Object> valor, float largura) {
            return new Coluna<>(titulo, valor, true, true, largura);
        }

        public static <T> Coluna<T> soCsv(String titulo, Function<T, Object> valor) {
            return new Coluna<>(titulo, valor, false, false, 0);
        }
    }
}
