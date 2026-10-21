package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.GrupoSalarialResposta;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.QuadroResposta;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.IndicadorRepository;
import br.edu.unidavi.rhgravatai.repository.IndicadorRepository.Agrupamento;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores de RH (RF03): quadro de pessoal por situação, regime e secretaria (FR-024,
 * FR-027) e salário base por cargo e nível (FR-025, FR-026). Os números do quadro também
 * alimentam os gráficos de distribuição (FR-029).
 */
@Service
public class IndicadorService {

    public static final Set<String> CAMPOS_ORDENACAO_SALARIO = IndicadorRepository.ORDENACOES_SALARIO.keySet();
    public static final String ORDENACAO_PADRAO_SALARIO = "soma,desc";

    private final IndicadorRepository indicadores;
    private final CargaRepository cargas;

    public IndicadorService(IndicadorRepository indicadores, CargaRepository cargas) {
        this.indicadores = indicadores;
        this.cargas = cargas;
    }

    @Transactional(readOnly = true)
    public QuadroResposta quadro(FiltroVinculos filtro) {
        var porSituacao = indicadores.contarPor(Agrupamento.SITUACAO, filtro);
        long total = porSituacao.stream().mapToLong(c -> c.quantidade()).sum();
        return new QuadroResposta(
                total,
                porSituacao,
                indicadores.contarPor(Agrupamento.REGIME, filtro),
                indicadores.contarPor(Agrupamento.SECRETARIA, filtro),
                cargas.ultimaAtualizacao());
    }

    /** Sem filtro de situação, considera só vínculos ativos (FR-026). */
    @Transactional(readOnly = true)
    public PaginaResposta<GrupoSalarialResposta> salarios(FiltroVinculos filtro, Paginacao paginacao) {
        return new PaginaResposta<>(indicadores.salarios(filtro, paginacao), indicadores.contarGruposSalariais(filtro),
                paginacao.pagina(), paginacao.tamanho(), cargas.ultimaAtualizacao());
    }

    public long contarGruposSalariais(FiltroVinculos filtro) {
        return indicadores.contarGruposSalariais(filtro);
    }

    /** Todos os grupos, para a exportação. Consumir dentro de uma transação. */
    public Stream<GrupoSalarialResposta> fluxoSalarios(FiltroVinculos filtro, Paginacao ordenacao) {
        return indicadores.fluxoSalarios(filtro, ordenacao);
    }
}
