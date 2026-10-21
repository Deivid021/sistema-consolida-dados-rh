package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.PaginaAlertasResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import br.edu.unidavi.rhgravatai.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vínculos sinalizados pelas regras de alerta (RF06). */
@Tag(name = "5. Alertas", description = "Salários acima do teto e fora do padrão (RF06)")
@RestController
public class AlertaController {

    private final AlertaService alertas;

    public AlertaController(AlertaService alertas) {
        this.alertas = alertas;
    }

    @Operation(operationId = "listarAlertas", summary = "Vínculos sinalizados para verificação",
            description = """
                    Alertas calculados na última carga, da maior para a menor diferença percentual. São
                    sinalizações para verificação, não afirmação de irregularidade. TETO: salário base acima
                    do subsídio do prefeito. FORA_PADRAO: mais de 20% de diferença da mediana dos vínculos
                    ativos com o mesmo cargo, nível e carga horária.""")
    @GetMapping("/api/v1/alertas")
    public PaginaAlertasResposta listar(@Parameter(description = "Filtra por regra; vazio traz as duas.")
                                        @RequestParam(required = false) RegraAlerta regra,
                                        @Parameter(description = "Busca em nome, cargo e matrícula, sem acento.")
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) List<String> regime,
                                        @RequestParam(required = false) List<String> secretaria,
                                        @RequestParam(required = false) List<String> cargo,
                                        @Parameter(description = "Página, começando em 0.", example = "0") @RequestParam(required = false) Integer pagina,
                                        @Parameter(description = "Itens por página (1 a 100). Padrão: 20.", example = "20") @RequestParam(required = false) Integer tamanho) {
        FiltroVinculos filtro = FiltroVinculos.de(regime, secretaria, cargo).comBusca(q);
        return alertas.listar(regra, filtro, Paginacao.de(pagina, tamanho));
    }
}
