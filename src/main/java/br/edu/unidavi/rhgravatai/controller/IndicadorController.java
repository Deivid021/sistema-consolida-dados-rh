package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.GrupoSalarialResposta;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.QuadroResposta;
import br.edu.unidavi.rhgravatai.service.IndicadorService;
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

    private final IndicadorService indicadores;

    public IndicadorController(IndicadorService indicadores) {
        this.indicadores = indicadores;
    }

    @GetMapping("/quadro")
    public QuadroResposta quadro(@ParameterObject @ModelAttribute FiltroVinculos filtro) {
        return indicadores.quadro(filtro);
    }

    @GetMapping("/salarios")
    public PaginaResposta<GrupoSalarialResposta> salarios(@ParameterObject @ModelAttribute FiltroVinculos filtro,
                                                          @RequestParam(required = false) Integer pagina,
                                                          @RequestParam(required = false) Integer tamanho,
                                                          @RequestParam(required = false) String ordenacao) {
        Paginacao paginacao = Paginacao.de(pagina, tamanho, ordenacao,
                IndicadorService.CAMPOS_ORDENACAO_SALARIO, IndicadorService.ORDENACAO_PADRAO_SALARIO);
        return indicadores.salarios(filtro, paginacao);
    }
}
