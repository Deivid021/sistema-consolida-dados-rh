package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.GrupoSalarialResposta;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.QuadroResposta;
import br.edu.unidavi.rhgravatai.service.IndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores de RH (RF03). Aceitam os mesmos filtros da consulta de vínculos. */
@Tag(name = "2. Indicadores", description = "Quadro de pessoal e salário base por cargo e nível (RF03)")
@RestController
@RequestMapping("/api/v1/indicadores")
public class IndicadorController {

    private final IndicadorService indicadores;

    public IndicadorController(IndicadorService indicadores) {
        this.indicadores = indicadores;
    }

    @Operation(operationId = "obterQuadro", summary = "Vínculos por situação, regime e secretaria",
            description = "Aceita os mesmos filtros da consulta. A soma de cada agrupamento é igual ao total. "
                    + "Também alimenta os gráficos de distribuição por regime e secretaria (RF04).")
    @GetMapping("/quadro")
    public QuadroResposta quadro(@ParameterObject @ModelAttribute FiltroVinculos filtro) {
        return indicadores.quadro(filtro);
    }

    @Operation(operationId = "obterSalarios", summary = "Quantidade, soma e média do salário base por cargo e nível",
            description = "Sem filtro de situação, considera só vínculos ativos. Salário zero ou ausente fica "
                    + "fora da soma e da média e é contado em quantidadeSemSalario.")
    @GetMapping("/salarios")
    public PaginaResposta<GrupoSalarialResposta> salarios(@ParameterObject @ModelAttribute FiltroVinculos filtro,
                                                          @Parameter(description = "Página, começando em 0.", example = "0") @RequestParam(required = false) Integer pagina,
                                                          @Parameter(description = "Itens por página (1 a 100). Padrão: 20.", example = "20") @RequestParam(required = false) Integer tamanho,
                                                          @Parameter(description = "Campo e direção, ex.: soma,desc. Campos: cargo, nivel, quantidade, soma, media. Padrão: soma,desc.")
                                                          @RequestParam(required = false) String ordenacao) {
        Paginacao paginacao = Paginacao.de(pagina, tamanho, ordenacao,
                IndicadorService.CAMPOS_ORDENACAO_SALARIO, IndicadorService.ORDENACAO_PADRAO_SALARIO);
        return indicadores.salarios(filtro, paginacao);
    }
}
