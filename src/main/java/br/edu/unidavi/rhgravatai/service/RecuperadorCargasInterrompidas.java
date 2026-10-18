package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Uma carga que estava rodando quando a aplicação parou nunca vai terminar: vira FALHA. */
@Component
public class RecuperadorCargasInterrompidas {

    private static final Logger log = LoggerFactory.getLogger(RecuperadorCargasInterrompidas.class);

    private final CargaRepository cargas;

    public RecuperadorCargasInterrompidas(CargaRepository cargas) {
        this.cargas = cargas;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void encerrarInterrompidas() {
        int encerradas = cargas.encerrarInterrompidas();
        if (encerradas > 0) {
            log.warn("{} carga(s) interrompida(s) marcada(s) como FALHA", encerradas);
        }
    }
}
