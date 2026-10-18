package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.StatusResposta;
import br.edu.unidavi.rhgravatai.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Situação dos dados publicados (FR-013). */
@Tag(name = "6. Situação dos dados", description = "Data da última atualização e teto em vigor")
@RestController
public class StatusController {

    private final StatusService status;

    public StatusController(StatusService status) {
        this.status = status;
    }

    @Operation(operationId = "obterStatus", summary = "Data da última carga, totais e teto em vigor",
            description = "O front-end deve exibir ultimaAtualizacao em todas as telas.")
    @GetMapping("/api/v1/status")
    public StatusResposta status() {
        return status.obter();
    }
}
