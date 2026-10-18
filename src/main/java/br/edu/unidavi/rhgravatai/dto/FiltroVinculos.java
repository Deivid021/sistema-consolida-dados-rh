package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.model.Situacao;
import br.edu.unidavi.rhgravatai.util.NormalizadorTexto;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros da consulta de vínculos (RF02), recebidos dos parâmetros da URL e reutilizados por
 * indicadores, gráficos, alertas e exportação. A conversão para SQL fica em
 * {@code repository.FiltroSql}.
 *
 * <p>Regras (FR-015 a FR-019): filtros diferentes são combinados com E e vários valores do
 * mesmo filtro com OU; {@value #NAO_INFORMADO} procura os vínculos sem o dado; os períodos
 * incluem as duas datas e aceitam só uma delas; o período de desligamento retorna só
 * desligados; a busca por texto ignora maiúsculas e acentos.
 */
public record FiltroVinculos(
        @Parameter(description = "Busca em nome, cargo e matrícula, sem diferenciar maiúsculas e acentos "
                + "(mínimo 2 caracteres).", example = "professor")
        String q,
        @Parameter(description = "Situação; repita o parâmetro para mais de uma.")
        List<Situacao> situacao,
        @Parameter(description = "Regime (ex.: ESTATUTARIO, CLT, APOSENTADO). Valores em /filtros/opcoes.")
        List<String> regime,
        @Parameter(description = "Classificação (ex.: PENSIONISTA). NAO_INFORMADO para os sem classificação.")
        List<String> classificacao,
        @Parameter(description = "Secretaria (centro de custo), com o nome exato de /filtros/opcoes.")
        List<String> secretaria,
        @Parameter(description = "Local de trabalho, com o nome exato de /filtros/opcoes.")
        List<String> localTrabalho,
        @Parameter(description = "Cargo, com o nome exato de /filtros/opcoes.")
        List<String> cargo,
        @Parameter(description = "Admitidos a partir desta data (yyyy-MM-dd).")
        LocalDate admissaoInicio,
        @Parameter(description = "Admitidos até esta data (yyyy-MM-dd).")
        LocalDate admissaoFim,
        @Parameter(description = "Desligados a partir desta data (yyyy-MM-dd); retorna só desligados.")
        LocalDate desligamentoInicio,
        @Parameter(description = "Desligados até esta data (yyyy-MM-dd); retorna só desligados.")
        LocalDate desligamentoFim) {

    public static final String NAO_INFORMADO = "NAO_INFORMADO";
    public static final int TAMANHO_MINIMO_BUSCA = 2;

    public static FiltroVinculos vazio() {
        return new FiltroVinculos(null, null, null, null, null, null, null, null, null, null, null);
    }

    public FiltroVinculos {
        situacao = situacao == null ? List.of() : List.copyOf(situacao);
        regime = valores(regime);
        classificacao = valores(classificacao);
        secretaria = valores(secretaria);
        localTrabalho = valores(localTrabalho);
        cargo = valores(cargo);
    }

    /** Filtro só por regime, secretaria e cargo, usado pelos gráficos e alertas (contrato). */
    public static FiltroVinculos de(List<String> regime, List<String> secretaria, List<String> cargo) {
        return new FiltroVinculos(null, null, regime, null, secretaria, null, cargo, null, null, null, null);
    }

    /** Devolve o filtro com a busca por texto trocada. */
    public FiltroVinculos comBusca(String novaBusca) {
        return new FiltroVinculos(novaBusca, situacao, regime, classificacao, secretaria, localTrabalho, cargo,
                admissaoInicio, admissaoFim, desligamentoInicio, desligamentoFim);
    }

    /** Devolve o filtro com a situação trocada, para indicadores que só consideram ativos. */
    public FiltroVinculos comSituacao(List<Situacao> novaSituacao) {
        return new FiltroVinculos(q, novaSituacao, regime, classificacao, secretaria, localTrabalho, cargo,
                admissaoInicio, admissaoFim, desligamentoInicio, desligamentoFim);
    }

    /**
     * Valida os filtros (FR-017, FR-019).
     *
     * @throws FiltroInvalidoException com mensagem para o usuário
     */
    public void validar() {
        validarPeriodo(admissaoInicio, admissaoFim, "admissão");
        validarPeriodo(desligamentoInicio, desligamentoFim, "desligamento");
        if (temBusca() && buscaNormalizada().length() < TAMANHO_MINIMO_BUSCA) {
            throw new FiltroInvalidoException(
                    "A busca precisa ter pelo menos " + TAMANHO_MINIMO_BUSCA + " caracteres.");
        }
    }

    public boolean temBusca() {
        return q != null && !q.isBlank();
    }

    /** Texto da busca sem acentos e em minúsculas, como a coluna texto_busca. */
    public String buscaNormalizada() {
        String normalizada = NormalizadorTexto.normalizar(q);
        return normalizada == null ? "" : normalizada;
    }

    public boolean temPeriodoDeDesligamento() {
        return desligamentoInicio != null || desligamentoFim != null;
    }

    /** Descreve os filtros aplicados em texto, para os relatórios exportados (FR-035). */
    public List<String> descrever() {
        List<String> descricao = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            descricao.add("Busca: " + q.strip());
        }
        if (!situacao.isEmpty()) {
            descricao.add("Situação: " + String.join(", ", situacao.stream().map(Enum::name).toList()));
        }
        adicionar(descricao, "Regime", regime);
        adicionar(descricao, "Classificação", classificacao);
        adicionar(descricao, "Secretaria", secretaria);
        adicionar(descricao, "Local de trabalho", localTrabalho);
        adicionar(descricao, "Cargo", cargo);
        if (admissaoInicio != null || admissaoFim != null) {
            descricao.add("Admissão: " + textoData(admissaoInicio) + " a " + textoData(admissaoFim));
        }
        if (desligamentoInicio != null || desligamentoFim != null) {
            descricao.add("Desligamento: " + textoData(desligamentoInicio) + " a " + textoData(desligamentoFim));
        }
        return descricao;
    }

    private static List<String> valores(List<String> lista) {
        if (lista == null) {
            return List.of();
        }
        return lista.stream().map(NormalizadorTexto::limpar).filter(v -> v != null).distinct().toList();
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim, String nome) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new FiltroInvalidoException(
                    "No período de " + nome + ", a data inicial não pode ser maior que a data final.");
        }
    }

    private static void adicionar(List<String> descricao, String rotulo, List<String> valores) {
        if (!valores.isEmpty()) {
            descricao.add(rotulo + ": " + String.join(", ",
                    valores.stream().map(v -> v.equals(NAO_INFORMADO) ? "Não informado" : v).toList()));
        }
    }

    private static String textoData(LocalDate data) {
        return data == null ? "…" : String.format("%02d/%02d/%04d", data.getDayOfMonth(), data.getMonthValue(), data.getYear());
    }

}
