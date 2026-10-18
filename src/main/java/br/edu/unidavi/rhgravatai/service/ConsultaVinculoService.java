package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.VinculoResposta;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.VinculoRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Busca e filtros de vínculos (RF02, FR-014 a FR-022). */
@Service
public class ConsultaVinculoService {

    public static final Set<String> CAMPOS_ORDENACAO = VinculoRepository.ORDENACOES.keySet();
    public static final String ORDENACAO_PADRAO = "nome,asc";

    private final VinculoRepository vinculos;
    private final CargaRepository cargas;

    public ConsultaVinculoService(VinculoRepository vinculos, CargaRepository cargas) {
        this.vinculos = vinculos;
        this.cargas = cargas;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<VinculoResposta> consultar(FiltroVinculos filtro, Paginacao paginacao) {
        long total = vinculos.contar(filtro);
        List<VinculoResposta> itens = vinculos.buscar(filtro, paginacao).stream().map(VinculoResposta::de).toList();
        return new PaginaResposta<>(itens, total, paginacao.pagina(), paginacao.tamanho(), cargas.ultimaAtualizacao());
    }

    public long contar(FiltroVinculos filtro) {
        return vinculos.contar(filtro);
    }

    /** Todos os vínculos do filtro, para a exportação (FR-034). Consumir dentro de uma transação. */
    public Stream<VinculoResposta> fluxo(FiltroVinculos filtro, Paginacao ordenacao) {
        return vinculos.fluxo(filtro, ordenacao).map(VinculoResposta::de);
    }
}
