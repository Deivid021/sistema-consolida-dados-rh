package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.config.PropriedadesAlertas;
import br.edu.unidavi.rhgravatai.model.Carga;
import br.edu.unidavi.rhgravatai.model.Teto;
import br.edu.unidavi.rhgravatai.repository.AlertaRepository;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Recalcula os alertas a cada carga publicada, dentro da mesma transação (RF06, FR-037 a
 * FR-041). Os alertas são sinalizações para verificação, não afirmação de irregularidade.
 */
@Service
public class CalculoAlertasService implements AposPublicacao {

    private static final Logger log = LoggerFactory.getLogger(CalculoAlertasService.class);

    private final AlertaRepository alertas;
    private final CargaRepository cargas;
    private final TetoService tetoService;
    private final PropriedadesAlertas propriedades;

    public CalculoAlertasService(AlertaRepository alertas, CargaRepository cargas, TetoService tetoService,
                                 PropriedadesAlertas propriedades) {
        this.alertas = alertas;
        this.cargas = cargas;
        this.tetoService = tetoService;
        this.propriedades = propriedades;
    }

    @Override
    public void aposPublicar(long cargaId) {
        Carga carga = cargas.buscar(cargaId).orElseThrow();
        alertas.apagarTodos();

        Teto teto = tetoService.resolver(carga.dataInicio());
        cargas.registrarTeto(cargaId, teto.valor(), teto.origem());
        int acimaDoTeto = teto.valor() == null ? 0 : alertas.inserirAcimaDoTeto(cargaId, teto.valor(), teto.prefeitoId());
        int foraDoPadrao = alertas.inserirForaDoPadrao(cargaId, propriedades.foraPadrao().percentual(),
                propriedades.foraPadrao().minimoGrupo());

        log.info("Carga {}: teto {} ({}); {} alerta(s) de teto e {} fora do padrão",
                cargaId, teto.valor(), teto.origem(), acimaDoTeto, foraDoPadrao);
    }
}
