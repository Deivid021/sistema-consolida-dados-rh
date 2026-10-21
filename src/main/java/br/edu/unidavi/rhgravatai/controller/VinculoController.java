package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.OpcoesFiltroResposta;
import br.edu.unidavi.rhgravatai.dto.PaginaResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.VinculoResposta;
import br.edu.unidavi.rhgravatai.service.ConsultaVinculoService;
import br.edu.unidavi.rhgravatai.service.OpcoesFiltroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de vínculos com busca e filtros (RF02). */
@Tag(name = "1. Consulta de vínculos", description = "Busca e filtros dos servidores (RF02)")
@RestController
@RequestMapping("/api/v1")
public class VinculoController {

    private final ConsultaVinculoService consulta;
    private final OpcoesFiltroService opcoes;

    public VinculoController(ConsultaVinculoService consulta, OpcoesFiltroService opcoes) {
        this.consulta = consulta;
        this.opcoes = opcoes;
    }

    @Operation(operationId = "listarVinculos", summary = "Busca e filtra vínculos",
            description = """
                    Lista paginada dos vínculos (matrículas). Filtros diferentes são combinados com E;
                    vários valores do mesmo filtro (parâmetro repetido) com OU. Use NAO_INFORMADO para
                    procurar vínculos sem o dado. Os valores possíveis vêm de GET /filtros/opcoes.""")
    @GetMapping("/vinculos")
    public PaginaResposta<VinculoResposta> listar(@ParameterObject @ModelAttribute FiltroVinculos filtro,
                                                  @Parameter(description = "Página, começando em 0.", example = "0") @RequestParam(required = false) Integer pagina,
                                                  @Parameter(description = "Itens por página (1 a 100). Padrão: 20.", example = "20") @RequestParam(required = false) Integer tamanho,
                                                  @Parameter(description = "Campo e direção, ex.: salarioBase,desc. Campos: nome, matricula, situacao, regime, secretaria, cargo, nivel, salarioBase, dataAdmissao, dataDesligamento. Padrão: nome,asc.")
                                                  @RequestParam(required = false) String ordenacao) {
        Paginacao paginacao = Paginacao.de(pagina, tamanho, ordenacao,
                ConsultaVinculoService.CAMPOS_ORDENACAO, ConsultaVinculoService.ORDENACAO_PADRAO);
        return consulta.consultar(filtro, paginacao);
    }

    @Operation(operationId = "listarOpcoesFiltro", summary = "Valores disponíveis para cada filtro",
            description = "Valores existentes nos dados, com a quantidade de vínculos. Valor nulo = \"Não informado\".")
    @GetMapping("/filtros/opcoes")
    public OpcoesFiltroResposta opcoes() {
        return opcoes.listar();
    }
}
