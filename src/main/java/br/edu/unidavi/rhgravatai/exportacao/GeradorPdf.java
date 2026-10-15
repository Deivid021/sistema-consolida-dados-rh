package br.edu.unidavi.rhgravatai.exportacao;

import br.edu.unidavi.rhgravatai.exportacao.Relatorio.Coluna;
import br.edu.unidavi.rhgravatai.status.StatusController;
import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * PDF em A4 paisagem com título, filtros, datas, tabela, total e fonte dos dados (FR-035).
 * O limite de linhas é verificado antes, pelo serviço de exportação (FR-036).
 */
@Component
public class GeradorPdf {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITULO = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9);
    private static final Font CABECALHO = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
    private static final Font CELULA = new Font(Font.HELVETICA, 7);
    private static final Font RODAPE = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY);
    private static final Color COR_CABECALHO = new Color(0x2F, 0x4F, 0x6F);

    public <T> void gerar(Relatorio<T> relatorio, OutputStream saida) {
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        try (Stream<T> linhas = relatorio.linhas()) {
            PdfWriter.getInstance(documento, saida);
            documento.open();

            documento.add(new Paragraph(relatorio.titulo(), TITULO));
            documento.add(new Paragraph(relatorio.filtros().isEmpty()
                    ? "Filtros: nenhum"
                    : "Filtros: " + String.join("; ", relatorio.filtros()), NORMAL));
            documento.add(new Paragraph("Dados atualizados em: " + dataHora(relatorio.ultimaAtualizacao())
                    + "    Gerado em: " + dataHora(OffsetDateTime.now()), NORMAL));
            documento.add(new Paragraph(" ", NORMAL));

            List<Coluna<T>> colunas = relatorio.colunas().stream().filter(Coluna::noPdf).toList();
            float[] larguras = new float[colunas.size()];
            for (int i = 0; i < colunas.size(); i++) {
                larguras[i] = colunas.get(i).largura();
            }
            PdfPTable tabela = new PdfPTable(larguras);
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            for (Coluna<T> coluna : colunas) {
                PdfPCell celula = new PdfPCell(new Phrase(coluna.titulo(), CABECALHO));
                celula.setBackgroundColor(COR_CABECALHO);
                tabela.addCell(celula);
            }
            for (T linha : (Iterable<T>) linhas::iterator) {
                for (Coluna<T> coluna : colunas) {
                    Object valor = coluna.valor().apply(linha);
                    PdfPCell celula = new PdfPCell(new Phrase(formatar(valor, coluna.moeda()), CELULA));
                    if (valor instanceof Number) {
                        celula.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    tabela.addCell(celula);
                }
            }
            documento.add(tabela);

            documento.add(new Paragraph(relatorio.rotuloTotal() + ": "
                    + NumberFormat.getIntegerInstance(BRASIL).format(relatorio.total()), NORMAL));
            documento.add(new Paragraph("Fonte: " + StatusController.FONTE, RODAPE));
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
    }

    static String formatar(Object valor, boolean moeda) {
        return switch (valor) {
            case null -> "";
            case BigDecimal numero when moeda ->
                    NumberFormat.getCurrencyInstance(BRASIL).format(numero).replace(' ', ' ');
            case BigDecimal numero -> NumberFormat.getNumberInstance(BRASIL).format(numero);
            case Number numero -> NumberFormat.getIntegerInstance(BRASIL).format(numero);
            case LocalDate data -> DATA.format(data);
            case Boolean b -> b ? "Sim" : "Não";
            default -> valor.toString();
        };
    }

    private static String dataHora(OffsetDateTime instante) {
        return instante == null ? "sem carga concluída" : DATA_HORA.format(instante.atZoneSameInstant(FUSO));
    }
}
