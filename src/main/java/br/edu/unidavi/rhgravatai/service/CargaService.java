package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.client.FontePessoal;
import br.edu.unidavi.rhgravatai.dto.ListaCargasResposta;
import br.edu.unidavi.rhgravatai.client.PaginaFonte;
import br.edu.unidavi.rhgravatai.client.RegistroFonte;
import br.edu.unidavi.rhgravatai.model.OrigemCarga;
import br.edu.unidavi.rhgravatai.model.StatusCarga;
import br.edu.unidavi.rhgravatai.model.Carga;
import br.edu.unidavi.rhgravatai.model.RegistroTratado;
import br.edu.unidavi.rhgravatai.model.RegistroRejeitado;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.PreparacaoRepository;
import br.edu.unidavi.rhgravatai.service.TratamentoRegistroService.Resultado;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Carga dos dados de pessoal (RF01; research.md §2). Executa uma carga completa:
 * lê todas as páginas para a área de preparação, concilia os totais com o que a fonte
 * informou e só então publica. Carga inconsistente ou com falha não altera os dados
 * publicados (FR-003).
 */
@Component
public class CargaService {

    /** Todo o histórico de desligados disponível na fonte (FR-002). */
    public static final LocalDate INICIO_HISTORICO = LocalDate.of(1900, 1, 1);

    private static final Logger log = LoggerFactory.getLogger(CargaService.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int LOG_A_CADA_PAGINAS = 50;

    private final FontePessoal fonte;
    private final TratamentoRegistroService tratador;
    private final CargaRepository cargas;
    private final PreparacaoRepository preparacao;
    private final PublicacaoCargaService publicador;

    public CargaService(FontePessoal fonte, TratamentoRegistroService tratador, CargaRepository cargas,
                         PreparacaoRepository preparacao, PublicacaoCargaService publicador) {
        this.fonte = fonte;
        this.tratador = tratador;
        this.cargas = cargas;
        this.preparacao = preparacao;
        this.publicador = publicador;
    }

    /**
     * Registra uma nova carga EM_ANDAMENTO.
     *
     * @throws br.edu.unidavi.rhgravatai.comum.erro.CargaEmAndamentoException se já houver uma
     */
    public Carga iniciar(OrigemCarga origem) {
        return cargas.criar(origem, INICIO_HISTORICO, LocalDate.now(FUSO));
    }

    /** Inicia e executa no mesmo fluxo (usado pelo agendamento). */
    public Carga executarCompleta(OrigemCarga origem) {
        Carga carga = iniciar(origem);
        executar(carga.id());
        return cargas.buscar(carga.id()).orElseThrow();
    }

    /** Cargas da mais recente para a mais antiga. */
    public ListaCargasResposta listar(int pagina, int tamanho) {
        return new ListaCargasResposta(cargas.listar(pagina, tamanho), cargas.contar());
    }

    /** Executa uma carga já iniciada. Nunca lança exceção: o resultado fica registrado na carga. */
    public void executar(long cargaId) {
        Carga carga = cargas.buscar(cargaId).orElseThrow();
        log.info("Carga {} iniciada ({}), desligamentos de {} a {}",
                cargaId, carga.origem(), carga.desligamentoInicio(), carga.desligamentoFim());
        try {
            preparacao.limpar();
            Integer totalInicial = null;
            Integer totalAtual = null;
            int totalPaginas = 1;

            for (int pagina = 1; pagina <= totalPaginas; pagina++) {
                PaginaFonte resposta = fonte.buscarPagina(pagina, carga.desligamentoInicio(), carga.desligamentoFim());
                if (pagina == 1) {
                    totalInicial = resposta.totalRegistros();
                    totalPaginas = Objects.requireNonNullElse(resposta.totalPaginas(), 0);
                    cargas.registrarTotais(cargaId, Objects.requireNonNullElse(totalInicial, 0), totalPaginas);
                }
                totalAtual = resposta.totalRegistros();
                int rejeitados = processarPagina(cargaId, pagina, resposta.registros());
                cargas.registrarPaginaLida(cargaId, pagina, rejeitados, totalAtual);
                if (pagina % LOG_A_CADA_PAGINAS == 0 || pagina == totalPaginas) {
                    log.info("Carga {}: página {} de {}", cargaId, pagina, totalPaginas);
                }
            }

            int gravados = preparacao.contarVinculosDistintos(cargaId);
            int linhas = preparacao.contarLinhas(cargaId);
            int rejeitados = preparacao.contarRejeitados(cargaId);
            cargas.registrarGravados(cargaId, gravados);

            String inconsistencia = conciliar(totalInicial, totalAtual, gravados, linhas, rejeitados);
            if (inconsistencia != null) {
                preparacao.limpar();
                cargas.encerrarSemPublicar(cargaId, StatusCarga.INCONSISTENTE, inconsistencia);
                log.warn("Carga {} inconsistente: {}", cargaId, inconsistencia);
                return;
            }

            publicador.publicar(cargaId);
            Carga concluida = cargas.buscar(cargaId).orElseThrow();
            log.info("Carga {} concluída: {} gravados, {} rejeitados, {} novos, {} alterados, {} não encontrados",
                    cargaId, gravados, rejeitados, concluida.novos(), concluida.alterados(), concluida.naoEncontrados());
        } catch (RuntimeException e) {
            String mensagem = Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
            preparacao.limpar();
            cargas.encerrarSemPublicar(cargaId, StatusCarga.FALHA, mensagem);
            log.error("Carga {} falhou: {}", cargaId, mensagem);
        }
    }

    private int processarPagina(long cargaId, int pagina, List<RegistroFonte> registros) {
        List<RegistroTratado> validos = new ArrayList<>();
        List<RegistroRejeitado> rejeicoes = new ArrayList<>();
        for (RegistroFonte registro : registros) {
            switch (tratador.tratar(registro)) {
                case Resultado.Valido v -> validos.add(v.registro());
                case Resultado.Rejeitado r -> rejeicoes.add(r.rejeicao());
            }
        }
        preparacao.inserir(cargaId, pagina, validos);
        preparacao.inserirRejeitados(cargaId, pagina, rejeicoes);
        return rejeicoes.size();
    }

    /** Devolve o motivo da inconsistência, ou nulo se os totais fecham. */
    static String conciliar(Integer totalInicial, Integer totalFinal, int gravados, int linhas, int rejeitados) {
        if (totalInicial == null || totalFinal == null) {
            return "A fonte não informou o total de registros.";
        }
        if (totalInicial == 0) {
            return "A fonte informou zero registros; os dados publicados foram mantidos.";
        }
        if (!totalInicial.equals(totalFinal)) {
            return "O total da fonte mudou durante a carga (de " + totalInicial + " para " + totalFinal + ").";
        }
        if (linhas != gravados) {
            return (linhas - gravados) + " matrícula(s) repetida(s) entre páginas; os dados mudaram durante a leitura.";
        }
        if (gravados + rejeitados != totalInicial) {
            return "Gravados (" + gravados + ") + rejeitados (" + rejeitados + ") diferente do total informado ("
                    + totalInicial + ").";
        }
        return null;
    }
}
