package br.edu.unidavi.rhgravatai.importacao.fonte;

import java.time.LocalDate;

/** Fonte dos dados de pessoal. A implementação real é {@link ClienteFonte}. */
public interface FontePessoal {

    /**
     * Busca uma página (começando em 1) com ativos e desligados no período de desligamento
     * informado.
     *
     * @throws FonteIndisponivelException se a página não puder ser obtida
     */
    PaginaFonte buscarPagina(int pagina, LocalDate desligamentoInicio, LocalDate desligamentoFim);
}
