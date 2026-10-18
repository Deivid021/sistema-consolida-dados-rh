package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.StatusResposta;
import br.edu.unidavi.rhgravatai.service.StatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Situação dos dados publicados (FR-013). */
@RestController
public class StatusController {

    private final StatusService status;

    public StatusController(StatusService status) {
        this.status = status;
    }

    @GetMapping("/api/v1/status")
    public StatusResposta status() {
        return status.obter();
    }
}
