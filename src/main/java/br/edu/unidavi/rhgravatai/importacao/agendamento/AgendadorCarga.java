package br.edu.unidavi.rhgravatai.importacao.agendamento;

import br.edu.unidavi.rhgravatai.comum.erro.CargaEmAndamentoException;
import br.edu.unidavi.rhgravatai.importacao.carga.Carga.OrigemCarga;
import br.edu.unidavi.rhgravatai.importacao.carga.ExecutorCarga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sincronização diária automática (FR-012). */
@Component
public class AgendadorCarga {

    private static final Logger log = LoggerFactory.getLogger(AgendadorCarga.class);

    private final ExecutorCarga executor;

    public AgendadorCarga(ExecutorCarga executor) {
        this.executor = executor;
    }

    @Scheduled(cron = "${importacao.cron}", zone = "America/Sao_Paulo")
    public void executarCargaDiaria() {
        try {
            executor.executarCompleta(OrigemCarga.AGENDADA);
        } catch (CargaEmAndamentoException e) {
            log.warn("Carga agendada não iniciada: já existe uma carga em andamento");
        }
    }
}
