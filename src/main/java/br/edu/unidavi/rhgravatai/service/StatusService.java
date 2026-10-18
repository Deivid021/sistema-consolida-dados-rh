package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.StatusResposta;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.VinculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Situação dos dados publicados: data da última carga, totais e teto em vigor (FR-013). */
@Service
public class StatusService {

    public static final String FONTE = "API de Dados Abertos de Pessoal - Prefeitura de Gravataí/RS";

    private final VinculoRepository vinculos;
    private final CargaRepository cargas;
    private final TetoService tetoService;

    public StatusService(VinculoRepository vinculos, CargaRepository cargas, TetoService tetoService) {
        this.vinculos = vinculos;
        this.cargas = cargas;
        this.tetoService = tetoService;
    }

    @Transactional(readOnly = true)
    public StatusResposta obter() {
        return new StatusResposta(cargas.ultimaAtualizacao(), vinculos.contarTodos(), vinculos.contarAtivos(), FONTE,
                tetoService.vigente());
    }
}
