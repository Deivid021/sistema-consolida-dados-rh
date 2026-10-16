package br.edu.unidavi.rhgravatai.indicador;

import br.edu.unidavi.rhgravatai.comum.PaginaResposta;
import br.edu.unidavi.rhgravatai.comum.Paginacao;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos;
import br.edu.unidavi.rhgravatai.indicador.QuadroService.Quadro;
import br.edu.unidavi.rhgravatai.indicador.SalarioService.GrupoSalarial;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores de RH (RF03). Aceitam os mesmos filtros da consulta de vínculos. */
@RestController
@RequestMapping("/api/v1/indicadores")
public class IndicadorController {

    private final QuadroService quadro;
    private final SalarioService salarios;

    public IndicadorController(QuadroService quadro, SalarioService salarios) {
        this.quadro = quadro;
        this.salarios = salarios;
    }

    @GetMapping("/quadro")
    public Quadro quadro(@ParameterObject @ModelAttribute FiltroVinculos filtro) {
        return quadro.calcular(filtro);
    }

    @GetMapping("/salarios")
    public PaginaResposta<GrupoSalarial> salarios(@ParameterObject @ModelAttribute FiltroVinculos filtro,
                                                  @RequestParam(required = false) Integer pagina,
                                                  @RequestParam(required = false) Integer tamanho,
                                                  @RequestParam(required = false) String ordenacao) {
        Paginacao paginacao = Paginacao.de(pagina, tamanho, ordenacao,
                SalarioService.ORDENACOES, SalarioService.ORDENACAO_PADRAO);
        return salarios.calcular(filtro, paginacao);
    }
}
