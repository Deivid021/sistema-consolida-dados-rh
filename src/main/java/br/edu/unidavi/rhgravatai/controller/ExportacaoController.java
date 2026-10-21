package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import br.edu.unidavi.rhgravatai.service.ExportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exportação em CSV e PDF (RF05). */
@Tag(name = "4. Exportação", description = "Relatórios em CSV e PDF (RF05)")
@RestController
public class ExportacaoController {

    private final ExportacaoService exportacao;

    public ExportacaoController(ExportacaoService exportacao) {
        this.exportacao = exportacao;
    }

    /**
     * {@code relatorio}: vinculos, quadro, salarios ou alertas. Aceita os mesmos filtros e a
     * mesma ordenação do endpoint correspondente; {@code regra} vale só para alertas.
     */
    @Operation(operationId = "exportar", summary = "Exporta um relatório em CSV ou PDF",
            description = """
                    Aceita os mesmos filtros e a mesma ordenação do endpoint correspondente. O CSV traz
                    todas as linhas (padrão brasileiro: separador ";" e vírgula decimal); o PDF é recusado
                    com 422 acima de 5.000 linhas. No Swagger, use o link "Download file" da resposta.""")
    @GetMapping("/api/v1/exportacoes/{relatorio}")
    public void exportar(@Parameter(description = "Relatório a exportar.",
                                    schema = @Schema(allowableValues = {"vinculos", "quadro", "salarios", "alertas"}))
                         @PathVariable String relatorio,
                         @Parameter(description = "Formato do arquivo.", required = true,
                                    schema = @Schema(allowableValues = {"csv", "pdf"}))
                         @RequestParam(required = false) String formato,
                         @ParameterObject @ModelAttribute FiltroVinculos filtro,
                         @Parameter(description = "Mesma ordenação do endpoint de origem (vinculos ou salarios).")
                         @RequestParam(required = false) String ordenacao,
                         @Parameter(description = "Só para o relatório de alertas.")
                         @RequestParam(required = false) RegraAlerta regra,
                         HttpServletResponse resposta) {
        exportacao.exportar(relatorio, formato, filtro, ordenacao, regra, resposta);
    }
}
