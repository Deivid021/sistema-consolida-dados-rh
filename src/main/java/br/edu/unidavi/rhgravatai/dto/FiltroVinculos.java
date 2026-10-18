package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.model.Situacao;
import br.edu.unidavi.rhgravatai.util.NormalizadorTexto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtros da consulta de vínculos (RF02), recebidos dos parâmetros da URL e reutilizados por
 * indicadores, gráficos, alertas e exportação.
 *
 * <p>Regras (FR-015 a FR-019): filtros diferentes são combinados com E e vários valores do
 * mesmo filtro com OU; {@value #NAO_INFORMADO} procura os vínculos sem o dado; os períodos
 * incluem as duas datas e aceitam só uma delas; o período de desligamento retorna só
 * desligados; a busca por texto ignora maiúsculas e acentos.
 */
public record FiltroVinculos(
        String q,
        List<Situacao> situacao,
        List<String> regime,
        List<String> classificacao,
        List<String> secretaria,
        List<String> localTrabalho,
        List<String> cargo,
        LocalDate admissaoInicio,
        LocalDate admissaoFim,
        LocalDate desligamentoInicio,
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

    /** Devolve o filtro com a situação trocada, para indicadores que só consideram ativos. */
    public FiltroVinculos comSituacao(List<Situacao> novaSituacao) {
        return new FiltroVinculos(q, novaSituacao, regime, classificacao, secretaria, localTrabalho, cargo,
                admissaoInicio, admissaoFim, desligamentoInicio, desligamentoFim);
    }

    /**
     * Monta a condição SQL. {@code prefixo} é o alias da tabela vinculo com ponto (ex.: "v.")
     * ou vazio. Lança {@link FiltroInvalidoException} se algum filtro for inválido.
     */
    public CondicaoSql paraSql(String prefixo) {
        validarPeriodo(admissaoInicio, admissaoFim, "admissão");
        validarPeriodo(desligamentoInicio, desligamentoFim, "desligamento");

        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (q != null && !q.isBlank()) {
            String busca = NormalizadorTexto.normalizar(q);
            if (busca == null || busca.length() < TAMANHO_MINIMO_BUSCA) {
                throw new FiltroInvalidoException(
                        "A busca precisa ter pelo menos " + TAMANHO_MINIMO_BUSCA + " caracteres.");
            }
            condicoes.add(prefixo + "texto_busca LIKE :fq");
            parametros.put("fq", "%" + escaparLike(busca) + "%");
        }

        if (!situacao.isEmpty()) {
            condicoes.add(prefixo + "situacao IN (:fsituacao)");
            parametros.put("fsituacao", situacao.stream().map(Enum::name).toList());
        }
        lista(condicoes, parametros, prefixo + "regime", "fregime", regime);
        lista(condicoes, parametros, prefixo + "classificacao", "fclassificacao", classificacao);
        lista(condicoes, parametros, prefixo + "secretaria", "fsecretaria", secretaria);
        lista(condicoes, parametros, prefixo + "local_trabalho", "flocal", localTrabalho);
        lista(condicoes, parametros, prefixo + "cargo", "fcargo", cargo);

        periodo(condicoes, parametros, prefixo + "data_admissao", "fadm", admissaoInicio, admissaoFim);
        if (desligamentoInicio != null || desligamentoFim != null) {
            condicoes.add(prefixo + "situacao = 'DESLIGADO'");
            periodo(condicoes, parametros, prefixo + "data_desligamento", "fdes", desligamentoInicio, desligamentoFim);
        }

        String where = condicoes.isEmpty() ? "TRUE" : String.join(" AND ", condicoes);
        return new CondicaoSql(where, parametros);
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

    private static void lista(List<String> condicoes, Map<String, Object> parametros,
                              String coluna, String nome, List<String> valores) {
        if (valores.isEmpty()) {
            return;
        }
        boolean incluiNaoInformado = valores.contains(NAO_INFORMADO);
        List<String> informados = valores.stream().filter(v -> !v.equals(NAO_INFORMADO)).toList();
        List<String> alternativas = new ArrayList<>();
        if (!informados.isEmpty()) {
            alternativas.add(coluna + " IN (:" + nome + ")");
            parametros.put(nome, informados);
        }
        if (incluiNaoInformado) {
            alternativas.add(coluna + " IS NULL");
        }
        condicoes.add("(" + String.join(" OR ", alternativas) + ")");
    }

    private static void periodo(List<String> condicoes, Map<String, Object> parametros,
                                String coluna, String nome, LocalDate inicio, LocalDate fim) {
        if (inicio != null) {
            condicoes.add(coluna + " >= :" + nome + "Inicio");
            parametros.put(nome + "Inicio", inicio);
        }
        if (fim != null) {
            condicoes.add(coluna + " <= :" + nome + "Fim");
            parametros.put(nome + "Fim", fim);
        }
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim, String nome) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new FiltroInvalidoException(
                    "No período de " + nome + ", a data inicial não pode ser maior que a data final.");
        }
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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

    /** Trecho de WHERE com seus parâmetros nomeados. */
    public record CondicaoSql(String where, Map<String, Object> parametros) {
    }
}
