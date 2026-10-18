package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Granularidade;
import br.edu.unidavi.rhgravatai.dto.MotivosRescisaoResposta;
import br.edu.unidavi.rhgravatai.dto.MovimentacaoResposta;
import br.edu.unidavi.rhgravatai.service.GraficoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gráficos de movimentação de pessoal (RF04). Aceitam os filtros de regime, secretaria e cargo. */
@RestController
@RequestMapping("/api/v1/graficos")
public class GraficoController {

    private final GraficoService graficos;

    public GraficoController(GraficoService graficos) {
        this.graficos = graficos;
    }

    @GetMapping("/movimentacao")
    public MovimentacaoResposta movimentacao(@RequestParam Granularidade granularidade,
                                             @RequestParam LocalDate inicio,
                                             @RequestParam LocalDate fim,
                                             @RequestParam(required = false) List<String> regime,
                                             @RequestParam(required = false) List<String> secretaria,
                                             @RequestParam(required = false) List<String> cargo) {
        return graficos.movimentacao(granularidade, inicio, fim, FiltroVinculos.de(regime, secretaria, cargo));
    }

    @GetMapping("/motivos-rescisao")
    public MotivosRescisaoResposta motivos(@RequestParam LocalDate inicio,
                                           @RequestParam LocalDate fim,
                                           @RequestParam(required = false) List<String> regime,
                                           @RequestParam(required = false) List<String> secretaria,
                                           @RequestParam(required = false) List<String> cargo) {
        return graficos.motivos(inicio, fim, FiltroVinculos.de(regime, secretaria, cargo));
    }
}
