package br.edu.unidavi.rhgravatai.importacao.fonte;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** Espera entre requisições. Existe para os testes não precisarem esperar de verdade. */
public interface Esperador {

    void esperar(Duration duracao);

    @Component
    class Real implements Esperador {

        @Override
        public void esperar(Duration duracao) {
            try {
                Thread.sleep(duracao);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FonteIndisponivelException("Carga interrompida durante a espera.");
            }
        }
    }
}
