package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.OpcoesFiltroResposta;
import br.edu.unidavi.rhgravatai.repository.VinculoRepository;
import br.edu.unidavi.rhgravatai.repository.VinculoRepository.ColunaOpcao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valores disponíveis para cada filtro, vindos dos dados importados (FR-016). */
@Service
public class OpcoesFiltroService {

    private final VinculoRepository vinculos;

    public OpcoesFiltroService(VinculoRepository vinculos) {
        this.vinculos = vinculos;
    }

    @Transactional(readOnly = true)
    public OpcoesFiltroResposta listar() {
        return new OpcoesFiltroResposta(
                vinculos.opcoes(ColunaOpcao.SITUACAO),
                vinculos.opcoes(ColunaOpcao.REGIME),
                vinculos.opcoes(ColunaOpcao.CLASSIFICACAO),
                vinculos.opcoes(ColunaOpcao.SECRETARIA),
                vinculos.opcoes(ColunaOpcao.LOCAL_TRABALHO),
                vinculos.opcoes(ColunaOpcao.CARGO));
    }
}
