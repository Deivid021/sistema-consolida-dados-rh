package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.MudancaVinculoRepository;
import br.edu.unidavi.rhgravatai.repository.PreparacaoRepository;
import br.edu.unidavi.rhgravatai.repository.VinculoRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica uma carga conciliada: tudo numa transação, para o site nunca ver dados pela metade
 * (FR-003, FR-008, FR-010, FR-011, FR-012).
 */
@Service
public class PublicacaoCargaService {

    private final VinculoRepository vinculos;
    private final MudancaVinculoRepository mudancas;
    private final PreparacaoRepository preparacao;
    private final CargaRepository cargas;
    private final ObjectProvider<AposPublicacao> etapasPosteriores;

    public PublicacaoCargaService(VinculoRepository vinculos, MudancaVinculoRepository mudancas,
                                  PreparacaoRepository preparacao, CargaRepository cargas,
                                  ObjectProvider<AposPublicacao> etapasPosteriores) {
        this.vinculos = vinculos;
        this.mudancas = mudancas;
        this.preparacao = preparacao;
        this.cargas = cargas;
        this.etapasPosteriores = etapasPosteriores;
    }

    @Transactional
    public void publicar(long cargaId) {
        // Na primeira carga tudo é novo: não há histórico de mudanças a registrar
        if (vinculos.existeAlgum()) {
            mudancas.registrarMudancasDaPreparacao(cargaId);
            mudancas.registrarRetornos(cargaId);
        }
        int alterados = mudancas.contarVinculosAlterados(cargaId);
        int novos = vinculos.contarNovosNaPreparacao(cargaId);

        vinculos.publicarPreparacao(cargaId);
        mudancas.registrarAusencias(cargaId);
        vinculos.marcarAusentes(cargaId);
        int naoEncontrados = vinculos.contarAusentes();

        etapasPosteriores.orderedStream().forEach(etapa -> etapa.aposPublicar(cargaId));

        preparacao.limpar(cargaId);
        cargas.concluirComSucesso(cargaId, novos, alterados, naoEncontrados);
    }
}
