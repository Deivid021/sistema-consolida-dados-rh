package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.AlertaResposta;
import br.edu.unidavi.rhgravatai.dto.Contagem;
import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.GrupoSalarialResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.QuadroResposta;
import br.edu.unidavi.rhgravatai.dto.Relatorio.Coluna;
import br.edu.unidavi.rhgravatai.dto.Relatorio;
import br.edu.unidavi.rhgravatai.dto.VinculoResposta;
import br.edu.unidavi.rhgravatai.exception.ExportacaoMuitoGrandeException;
import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exportação das consultas e indicadores em CSV e PDF (RF05, FR-033 a FR-036). Cada relatório
 * usa os mesmos filtros e a mesma ordenação do endpoint correspondente.
 */
@Service
public class ExportacaoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ConsultaVinculoService vinculos;
    private final IndicadorService indicadores;
    private final AlertaService alertas;
    private final CargaRepository cargas;
    private final EscritorCsv csv;
    private final GeradorPdf pdf;
    private final int maximoLinhasPdf;

    public ExportacaoService(ConsultaVinculoService vinculos, IndicadorService indicadores, AlertaService alertas,
                             CargaRepository cargas, EscritorCsv csv, GeradorPdf pdf,
                             @Value("${exportacao.pdf.max-linhas}") int maximoLinhasPdf) {
        this.vinculos = vinculos;
        this.indicadores = indicadores;
        this.alertas = alertas;
        this.cargas = cargas;
        this.csv = csv;
        this.pdf = pdf;
        this.maximoLinhasPdf = maximoLinhasPdf;
    }

    /**
     * Valida, conta as linhas e só então define os cabeçalhos e escreve o arquivo; assim um
     * erro de filtro ou de limite ainda vira uma resposta de erro normal.
     */
    @Transactional(readOnly = true)
    public void exportar(String tipo, String formato, FiltroVinculos filtro, String ordenacao, RegraAlerta regra,
                         HttpServletResponse resposta) {
        TipoRelatorio relatorioPedido = TipoRelatorio.de(tipo);
        Formato formatoPedido = Formato.de(formato);
        Relatorio<?> relatorio = switch (relatorioPedido) {
            case VINCULOS -> vinculos(filtro, ordenacao, formatoPedido);
            case QUADRO -> quadro(filtro);
            case SALARIOS -> salarios(filtro, ordenacao, formatoPedido);
            case ALERTAS -> alertas(filtro, regra, formatoPedido);
        };

        String nome = relatorio.nomeArquivo() + "-" + LocalDate.now(FUSO) + "." + formatoPedido.extensao;
        resposta.setContentType(formatoPedido.tipoConteudo);
        resposta.setHeader("Content-Disposition", "attachment; filename=\"" + nome + "\"");
        try {
            if (formatoPedido == Formato.CSV) {
                csv.escrever(relatorio, resposta.getOutputStream());
            } else {
                pdf.gerar(relatorio, resposta.getOutputStream());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Relatorio<VinculoResposta> vinculos(FiltroVinculos filtro, String ordenacao, Formato formato) {
        Paginacao ordem = Paginacao.de(0, 1, ordenacao, ConsultaVinculoService.CAMPOS_ORDENACAO,
                ConsultaVinculoService.ORDENACAO_PADRAO);
        long total = vinculos.contar(filtro);
        verificarLimite(formato, total);
        List<Coluna<VinculoResposta>> colunas = List.of(
                Coluna.texto("Matrícula", VinculoResposta::matricula, 6),
                Coluna.texto("Nome", VinculoResposta::nome, 16),
                Coluna.texto("Situação", v -> v.situacao().name(), 8),
                Coluna.texto("Regime", VinculoResposta::regime, 8),
                Coluna.soCsv("Classificação", VinculoResposta::classificacao),
                Coluna.texto("Secretaria", VinculoResposta::secretaria, 14),
                Coluna.soCsv("Local de trabalho", VinculoResposta::localTrabalho),
                Coluna.texto("Cargo", VinculoResposta::cargo, 14),
                Coluna.soCsv("Função", VinculoResposta::funcao),
                Coluna.texto("Nível", VinculoResposta::nivel, 5),
                Coluna.moeda("Salário base", VinculoResposta::salarioBase, 8),
                Coluna.soCsv("Horas mensais", VinculoResposta::horasMensais),
                Coluna.texto("Admissão", VinculoResposta::dataAdmissao, 6),
                Coluna.texto("Desligamento", VinculoResposta::dataDesligamento, 6),
                Coluna.soCsv("Motivo do desligamento", VinculoResposta::motivoDesligamento),
                Coluna.soCsv("Presente na última carga", VinculoResposta::ativoNaFonte));
        return new Relatorio<>("Consulta de vínculos", "vinculos", filtro.descrever(), cargas.ultimaAtualizacao(),
                colunas, vinculos.fluxo(filtro, ordem), "Total de vínculos", total);
    }

    private Relatorio<LinhaQuadro> quadro(FiltroVinculos filtro) {
        QuadroResposta resultado = indicadores.quadro(filtro);
        List<LinhaQuadro> linhas = new ArrayList<>();
        adicionar(linhas, "Situação", resultado.porSituacao());
        adicionar(linhas, "Regime", resultado.porRegime());
        adicionar(linhas, "Secretaria", resultado.porSecretaria());
        List<Coluna<LinhaQuadro>> colunas = List.of(
                Coluna.texto("Agrupamento", LinhaQuadro::agrupamento, 4),
                Coluna.texto("Valor", l -> l.valor() == null ? "Não informado" : l.valor(), 12),
                Coluna.texto("Quantidade de vínculos", LinhaQuadro::quantidade, 4));
        return new Relatorio<>("Quadro de pessoal por situação, regime e secretaria", "quadro", filtro.descrever(),
                resultado.ultimaAtualizacao(), colunas, linhas.stream(), "Total de vínculos", resultado.total());
    }

    private Relatorio<GrupoSalarialResposta> salarios(FiltroVinculos filtro, String ordenacao, Formato formato) {
        Paginacao ordem = Paginacao.de(0, 1, ordenacao, IndicadorService.CAMPOS_ORDENACAO_SALARIO,
                IndicadorService.ORDENACAO_PADRAO_SALARIO);
        long total = indicadores.contarGruposSalariais(filtro);
        verificarLimite(formato, total);
        List<String> filtros = new ArrayList<>(filtro.descrever());
        if (filtro.situacao().isEmpty()) {
            filtros.add("Somente vínculos ativos");
        }
        List<Coluna<GrupoSalarialResposta>> colunas = List.of(
                Coluna.texto("Cargo", GrupoSalarialResposta::cargo, 16),
                Coluna.texto("Nível", GrupoSalarialResposta::nivel, 6),
                Coluna.texto("Vínculos com salário", GrupoSalarialResposta::quantidade, 5),
                Coluna.texto("Sem salário", GrupoSalarialResposta::quantidadeSemSalario, 4),
                Coluna.moeda("Soma do salário base", GrupoSalarialResposta::soma, 8),
                Coluna.moeda("Média do salário base", GrupoSalarialResposta::media, 8));
        return new Relatorio<>("Salário base por cargo e nível", "salarios", filtros, cargas.ultimaAtualizacao(),
                colunas, indicadores.fluxoSalarios(filtro, ordem), "Total de grupos", total);
    }

    private Relatorio<AlertaResposta> alertas(FiltroVinculos filtro, RegraAlerta regra, Formato formato) {
        long total = alertas.contar(regra, filtro);
        verificarLimite(formato, total);
        List<String> filtros = new ArrayList<>(filtro.descrever());
        if (regra != null) {
            filtros.add("Regra: " + regra.name());
        }
        List<Coluna<AlertaResposta>> colunas = List.of(
                Coluna.texto("Regra", a -> a.regra().name(), 5),
                Coluna.texto("Matrícula", a -> a.vinculo().matricula(), 5),
                Coluna.texto("Nome", a -> a.vinculo().nome(), 12),
                Coluna.texto("Cargo", a -> a.vinculo().cargo(), 10),
                Coluna.texto("Nível", a -> a.vinculo().nivel(), 4),
                Coluna.texto("Secretaria", a -> a.vinculo().secretaria(), 10),
                Coluna.moeda("Salário base", AlertaResposta::valorVinculo, 6),
                Coluna.moeda("Referência", AlertaResposta::valorReferencia, 6),
                Coluna.texto("Diferença (%)", AlertaResposta::diferencaPercentual, 4),
                Coluna.texto("Critério", AlertaResposta::descricaoRegra, 22));
        return new Relatorio<>("Alertas para verificação (não indicam irregularidade)", "alertas", filtros,
                cargas.ultimaAtualizacao(), colunas, alertas.fluxo(regra, filtro), "Total de alertas", total);
    }

    private void verificarLimite(Formato formato, long linhas) {
        if (formato == Formato.PDF && linhas > maximoLinhasPdf) {
            throw new ExportacaoMuitoGrandeException(linhas, maximoLinhasPdf);
        }
    }

    private static void adicionar(List<LinhaQuadro> linhas, String agrupamento, List<Contagem> contagens) {
        contagens.forEach(c -> linhas.add(new LinhaQuadro(agrupamento, c.valor(), c.quantidade())));
    }

    record LinhaQuadro(String agrupamento, String valor, long quantidade) {
    }

    enum TipoRelatorio {
        VINCULOS, QUADRO, SALARIOS, ALERTAS;

        static TipoRelatorio de(String texto) {
            return Stream.of(values()).filter(t -> t.name().equalsIgnoreCase(texto)).findFirst()
                    .orElseThrow(() -> new FiltroInvalidoException(
                            "Relatório inválido: use vinculos, quadro, salarios ou alertas."));
        }
    }

    enum Formato {
        CSV("csv", "text/csv;charset=UTF-8"),
        PDF("pdf", "application/pdf");

        private final String extensao;
        private final String tipoConteudo;

        Formato(String extensao, String tipoConteudo) {
            this.extensao = extensao;
            this.tipoConteudo = tipoConteudo;
        }

        static Formato de(String texto) {
            if (texto == null) {
                throw new FiltroInvalidoException("O parâmetro 'formato' é obrigatório: csv ou pdf.");
            }
            return switch (texto.toLowerCase(Locale.ROOT)) {
                case "csv" -> CSV;
                case "pdf" -> PDF;
                default -> throw new FiltroInvalidoException("Formato inválido: use csv ou pdf.");
            };
        }
    }
}
