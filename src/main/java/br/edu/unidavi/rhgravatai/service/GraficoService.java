package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Granularidade;
import br.edu.unidavi.rhgravatai.dto.MotivoRescisao;
import br.edu.unidavi.rhgravatai.dto.MotivosRescisaoResposta;
import br.edu.unidavi.rhgravatai.dto.MovimentacaoResposta;
import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.GraficoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gráficos de movimentação de pessoal (RF04). A distribuição por regime e secretaria vem do
 * quadro de pessoal ({@link IndicadorService#quadro}).
 */
@Service
public class GraficoService {

    static final int MAXIMO_MOTIVOS = 10;
    static final String OUTROS = "Outros";
    static final String NAO_INFORMADO = "Não informado";

    private final GraficoRepository graficos;
    private final CargaRepository cargas;

    public GraficoService(GraficoRepository graficos, CargaRepository cargas) {
        this.graficos = graficos;
        this.cargas = cargas;
    }

    /** Admissões e desligamentos por mês ou ano, incluindo períodos sem movimentação (FR-030). */
    @Transactional(readOnly = true)
    public MovimentacaoResposta movimentacao(Granularidade granularidade, LocalDate inicio, LocalDate fim,
                                             FiltroVinculos filtro) {
        validar(inicio, fim);
        return new MovimentacaoResposta(granularidade, graficos.movimentacao(granularidade, inicio, fim, filtro),
                cargas.ultimaAtualizacao());
    }

    /**
     * Os dez motivos de desligamento mais frequentes no período; os demais somados em
     * "Outros" e os ausentes em "Não informado" (FR-031).
     */
    @Transactional(readOnly = true)
    public MotivosRescisaoResposta motivos(LocalDate inicio, LocalDate fim, FiltroVinculos filtro) {
        validar(inicio, fim);
        List<MotivoRescisao> motivos = new ArrayList<>();
        long outros = 0;
        long naoInformado = 0;
        long total = 0;
        for (MotivoRescisao grupo : graficos.motivosDeDesligamento(inicio, fim, filtro)) {
            total += grupo.quantidade();
            if (grupo.motivo() == null) {
                naoInformado += grupo.quantidade();
            } else if (motivos.size() < MAXIMO_MOTIVOS) {
                motivos.add(grupo);
            } else {
                outros += grupo.quantidade();
            }
        }
        if (outros > 0) {
            motivos.add(new MotivoRescisao(OUTROS, outros));
        }
        if (naoInformado > 0) {
            motivos.add(new MotivoRescisao(NAO_INFORMADO, naoInformado));
        }
        return new MotivosRescisaoResposta(total, motivos, cargas.ultimaAtualizacao());
    }

    private static void validar(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new FiltroInvalidoException("A data inicial não pode ser maior que a data final.");
        }
    }
}
