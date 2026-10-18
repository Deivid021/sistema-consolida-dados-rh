package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.service.GraficoService.Granularidade;
import br.edu.unidavi.rhgravatai.service.GraficoService.MotivosRescisao;
import br.edu.unidavi.rhgravatai.service.GraficoService.Movimentacao;
import br.edu.unidavi.rhgravatai.service.GraficoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gráficos de movimentação de pessoal (RF04). */
@RestController
@RequestMapping("/api/v1/graficos")
public class GraficoController {

    private final GraficoService graficos;

    public GraficoController(GraficoService graficos) {
        this.graficos = graficos;
    }

    @GetMapping("/movimentacao")
    public Movimentacao movimentacao(@RequestParam Granularidade granularidade,
                                     @RequestParam LocalDate inicio,
                                     @RequestParam LocalDate fim,
                                     @RequestParam(required = false) List<String> regime,
                                     @RequestParam(required = false) List<String> secretaria,
                                     @RequestParam(required = false) List<String> cargo) {
        return graficos.movimentacao(granularidade, inicio, fim, filtro(regime, secretaria, cargo));
    }

    @GetMapping("/motivos-rescisao")
    public MotivosRescisao motivos(@RequestParam LocalDate inicio,
                                   @RequestParam LocalDate fim,
                                   @RequestParam(required = false) List<String> regime,
                                   @RequestParam(required = false) List<String> secretaria,
                                   @RequestParam(required = false) List<String> cargo) {
        return graficos.motivos(inicio, fim, filtro(regime, secretaria, cargo));
    }

    /** Os gráficos aceitam só os filtros de regime, secretaria e cargo (contrato). */
    private static FiltroVinculos filtro(List<String> regime, List<String> secretaria, List<String> cargo) {
        return new FiltroVinculos(null, null, regime, null, secretaria, null, cargo, null, null, null, null);
    }
}
