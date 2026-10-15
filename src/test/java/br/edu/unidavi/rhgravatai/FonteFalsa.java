package br.edu.unidavi.rhgravatai;

import br.edu.unidavi.rhgravatai.importacao.fonte.FonteIndisponivelException;
import br.edu.unidavi.rhgravatai.importacao.fonte.FontePessoal;
import br.edu.unidavi.rhgravatai.importacao.fonte.PaginaFonte;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fonte simulada para os testes de integração: devolve as páginas JSON de
 * src/test/resources/fonte/. Os testes nunca acessam a API real.
 */
public class FonteFalsa implements FontePessoal {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private List<String> arquivos = List.of();
    private int paginaComFalha = -1;

    /** Arquivos das páginas, na ordem (o primeiro é a página 1). */
    public void usar(String... arquivosDasPaginas) {
        this.arquivos = List.of(arquivosDasPaginas);
        this.paginaComFalha = -1;
    }

    public void falharNaPagina(int pagina) {
        this.paginaComFalha = pagina;
    }

    @Override
    public PaginaFonte buscarPagina(int pagina, LocalDate desligamentoInicio, LocalDate desligamentoFim) {
        if (pagina == paginaComFalha) {
            throw new FonteIndisponivelException("Página " + pagina + " indisponível após 5 tentativas (HTTP 503).");
        }
        try {
            String conteudo = new ClassPathResource("fonte/" + arquivos.get(pagina - 1))
                    .getContentAsString(StandardCharsets.UTF_8);
            return JSON.readValue(conteudo, PaginaFonte.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
