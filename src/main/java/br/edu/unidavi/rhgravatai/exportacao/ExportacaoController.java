package br.edu.unidavi.rhgravatai.exportacao;

import br.edu.unidavi.rhgravatai.alerta.RegraAlerta;
import br.edu.unidavi.rhgravatai.comum.filtro.FiltroVinculos;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exportação em CSV e PDF (RF05). */
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
    @GetMapping("/api/v1/exportacoes/{relatorio}")
    public void exportar(@PathVariable String relatorio,
                         @RequestParam(required = false) String formato,
                         @ParameterObject @ModelAttribute FiltroVinculos filtro,
                         @RequestParam(required = false) String ordenacao,
                         @RequestParam(required = false) RegraAlerta regra,
                         HttpServletResponse resposta) {
        exportacao.exportar(relatorio, formato, filtro, ordenacao, regra, resposta);
    }
}
