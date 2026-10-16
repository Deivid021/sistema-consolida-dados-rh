package br.edu.unidavi.rhgravatai.vinculo;

import br.edu.unidavi.rhgravatai.comum.PaginaResposta;
import br.edu.unidavi.rhgravatai.comum.Paginacao;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos;
import br.edu.unidavi.rhgravatai.vinculo.OpcoesFiltroService.OpcoesFiltro;
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
                ConsultaVinculoService.ORDENACOES, ConsultaVinculoService.ORDENACAO_PADRAO);
        return consulta.consultar(filtro, paginacao);
    }

    @GetMapping("/filtros/opcoes")
    public OpcoesFiltro opcoes() {
        return opcoes.listar();
    }
}
