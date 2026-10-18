package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.OpcoesFiltroResposta;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.VinculoResposta;
import br.edu.unidavi.rhgravatai.service.ConsultaVinculoService;
import br.edu.unidavi.rhgravatai.service.OpcoesFiltroService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de vínculos com busca e filtros (RF02). */
@RestController
@RequestMapping("/api/v1")
public class VinculoController {

    private final ConsultaVinculoService consulta;
    private final OpcoesFiltroService opcoes;

    public VinculoController(ConsultaVinculoService consulta, OpcoesFiltroService opcoes) {
        this.consulta = consulta;
        this.opcoes = opcoes;
    }

    /** Os filtros vêm dos parâmetros da URL; listas aceitam o parâmetro repetido. */
    @GetMapping("/vinculos")
    public PaginaResposta<VinculoResposta> listar(@ParameterObject @ModelAttribute FiltroVinculos filtro,
                                                  @RequestParam(required = false) Integer pagina,
                                                  @RequestParam(required = false) Integer tamanho,
                                                  @RequestParam(required = false) String ordenacao) {
        Paginacao paginacao = Paginacao.de(pagina, tamanho, ordenacao,
                ConsultaVinculoService.CAMPOS_ORDENACAO, ConsultaVinculoService.ORDENACAO_PADRAO);
        return consulta.consultar(filtro, paginacao);
    }

    @GetMapping("/filtros/opcoes")
    public OpcoesFiltroResposta opcoes() {
        return opcoes.listar();
    }
}
