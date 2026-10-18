package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Granularidade;
import br.edu.unidavi.rhgravatai.dto.MotivosRescisaoResposta;
import br.edu.unidavi.rhgravatai.dto.MovimentacaoResposta;
import br.edu.unidavi.rhgravatai.service.GraficoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gráficos de movimentação de pessoal (RF04). Aceitam os filtros de regime, secretaria e cargo. */
@Tag(name = "3. Gráficos", description = "Movimentação de pessoal e motivos de desligamento (RF04)")
@RestController
@RequestMapping("/api/v1/graficos")
public class GraficoController {

    private final GraficoService graficos;

    public GraficoController(GraficoService graficos) {
        this.graficos = graficos;
    }

    @Operation(operationId = "obterMovimentacao", summary = "Admissões e desligamentos por mês ou ano",
            description = "Inclui os períodos sem movimentação, com zero.")
    @GetMapping("/movimentacao")
    public MovimentacaoResposta movimentacao(@Parameter(description = "MES (períodos yyyy-MM) ou ANO (yyyy).") @RequestParam Granularidade granularidade,
                                             @Parameter(description = "Data inicial (yyyy-MM-dd).", example = "2026-01-01") @RequestParam LocalDate inicio,
                                             @Parameter(description = "Data final (yyyy-MM-dd).", example = "2026-12-31") @RequestParam LocalDate fim,
                                             @RequestParam(required = false) List<String> regime,
                                             @RequestParam(required = false) List<String> secretaria,
                                             @RequestParam(required = false) List<String> cargo) {
        return graficos.movimentacao(granularidade, inicio, fim, FiltroVinculos.de(regime, secretaria, cargo));
    }

    @Operation(operationId = "obterMotivosRescisao", summary = "Dez motivos de desligamento mais frequentes",
            description = "Os demais motivos são somados em \"Outros\" e os ausentes em \"Não informado\".")
    @GetMapping("/motivos-rescisao")
    public MotivosRescisaoResposta motivos(@Parameter(description = "Data inicial (yyyy-MM-dd).", example = "2026-01-01") @RequestParam LocalDate inicio,
                                           @Parameter(description = "Data final (yyyy-MM-dd).", example = "2026-12-31") @RequestParam LocalDate fim,
                                           @RequestParam(required = false) List<String> regime,
                                           @RequestParam(required = false) List<String> secretaria,
                                           @RequestParam(required = false) List<String> cargo) {
        return graficos.motivos(inicio, fim, FiltroVinculos.de(regime, secretaria, cargo));
    }
}
