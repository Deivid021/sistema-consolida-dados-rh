package br.edu.unidavi.rhgravatai.exportacao;

import br.edu.unidavi.rhgravatai.exportacao.Relatorio.Coluna;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * CSV no padrão que o Excel em português abre direto: UTF-8 com BOM, separador ";", vírgula
 * decimal e datas dd/MM/yyyy (FR-034). Escreve linha a linha, sem carregar tudo na memória.
 */
@Component
public class EscritorCsv {

    private static final char BOM = '﻿';
    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public <T> void escrever(Relatorio<T> relatorio, OutputStream saida) {
        Writer escritor = new BufferedWriter(new OutputStreamWriter(saida, StandardCharsets.UTF_8));
        try (Stream<T> linhas = relatorio.linhas()) {
            escritor.write(BOM);
            List<Coluna<T>> colunas = relatorio.colunas();
            escritor.write(String.join(SEPARADOR, colunas.stream().map(c -> campo(c.titulo())).toList()));
            escritor.write("\r\n");
            for (T linha : (Iterable<T>) linhas::iterator) {
                escritor.write(String.join(SEPARADOR, colunas.stream().map(c -> campo(formatar(c.valor().apply(linha)))).toList()));
                escritor.write("\r\n");
            }
            escritor.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String formatar(Object valor) {
        return switch (valor) {
            case null -> "";
            case BigDecimal numero -> numero.toPlainString().replace('.', ',');
            case LocalDate data -> DATA.format(data);
            case Boolean b -> b ? "Sim" : "Não";
            default -> valor.toString();
        };
    }

    private static String campo(String texto) {
        if (texto.contains(SEPARADOR) || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
