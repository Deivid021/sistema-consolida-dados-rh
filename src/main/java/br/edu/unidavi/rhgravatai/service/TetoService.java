package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.dto.TetoResposta;
import br.edu.unidavi.rhgravatai.model.Carga;
import br.edu.unidavi.rhgravatai.model.OrigemTeto;
import br.edu.unidavi.rhgravatai.model.ParametroTeto;
import br.edu.unidavi.rhgravatai.model.Teto;
import br.edu.unidavi.rhgravatai.model.Vinculo;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.repository.ParametroTetoRepository;
import br.edu.unidavi.rhgravatai.repository.VinculoRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Teto municipal (FR-037). A ordem de prioridade é:
 * <ol>
 *   <li>valor manual em parametro_teto com a maior vigência não posterior à data;</li>
 *   <li>senão, salário base do vínculo ativo de cargo PREFEITO publicado pela fonte;</li>
 *   <li>senão, a regra fica suspensa.</li>
 * </ol>
 * O vínculo do prefeito é sempre identificado, porque nunca deve ser sinalizado.
 */
@Service
public class TetoService {

    public static final String CARGO_PREFEITO = "PREFEITO";
    static final String FUNDAMENTO_FONTE = "Salário base do cargo PREFEITO publicado pela fonte";

    private final VinculoRepository vinculos;
    private final ParametroTetoRepository parametros;
    private final CargaRepository cargas;

    public TetoService(VinculoRepository vinculos, ParametroTetoRepository parametros, CargaRepository cargas) {
        this.vinculos = vinculos;
        this.parametros = parametros;
        this.cargas = cargas;
    }

    /** Define o teto a usar no cálculo dos alertas de uma carga. */
    public Teto resolver(LocalDate data) {
        Optional<Vinculo> prefeito = vinculos.buscarPrefeitoAtivo(CARGO_PREFEITO);
        Long prefeitoId = prefeito.map(Vinculo::id).orElse(null);
        Optional<ParametroTeto> manual = parametros.vigenteEm(data);
        if (manual.isPresent()) {
            return new Teto(manual.get().valor(), OrigemTeto.MANUAL, manual.get().fundamento(), prefeitoId);
        }
        return prefeito
                .map(p -> new Teto(p.salarioBase(), OrigemTeto.FONTE, FUNDAMENTO_FONTE, p.id()))
                .orElse(Teto.suspenso());
    }

    /** Teto usado na última carga publicada, exibido em /status e /alertas. */
    public TetoResposta vigente() {
        Carga ultima = cargas.ultimaComSucesso().orElse(null);
        if (ultima == null || ultima.tetoOrigem() == null || ultima.tetoOrigem() == OrigemTeto.SUSPENSA) {
            return new TetoResposta(null, OrigemTeto.SUSPENSA, null);
        }
        String fundamento = ultima.tetoOrigem() == OrigemTeto.FONTE
                ? FUNDAMENTO_FONTE
                : parametros.vigenteEm(ultima.dataInicio()).map(ParametroTeto::fundamento).orElse(null);
        return new TetoResposta(ultima.tetoValor(), ultima.tetoOrigem(), fundamento);
    }
}
