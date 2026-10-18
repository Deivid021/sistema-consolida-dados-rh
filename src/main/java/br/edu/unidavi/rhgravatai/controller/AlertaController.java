package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.PaginaAlertasResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import br.edu.unidavi.rhgravatai.service.AlertaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vínculos sinalizados pelas regras de alerta (RF06). */
@RestController
public class AlertaController {

    private final AlertaService alertas;

    public AlertaController(AlertaService alertas) {
        this.alertas = alertas;
    }

    @GetMapping("/api/v1/alertas")
    public PaginaAlertasResposta listar(@RequestParam(required = false) RegraAlerta regra,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) List<String> regime,
                                        @RequestParam(required = false) List<String> secretaria,
                                        @RequestParam(required = false) List<String> cargo,
                                        @RequestParam(required = false) Integer pagina,
                                        @RequestParam(required = false) Integer tamanho) {
        FiltroVinculos filtro = FiltroVinculos.de(regime, secretaria, cargo).comBusca(q);
        return alertas.listar(regra, filtro, Paginacao.de(pagina, tamanho));
    }
}
