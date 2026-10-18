package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import java.util.Map;

/**
 * Página, tamanho e ordenação validados. A ordenação só aceita os campos da lista recebida,
 * que mapeia o nome do contrato para a coluna do banco; assim nenhum texto do usuário chega
 * ao SQL.
 */
public record Paginacao(int pagina, int tamanho, String orderBy) {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    public static Paginacao de(Integer pagina, Integer tamanho, String ordenacao,
                               Map<String, String> colunasPermitidas, String ordenacaoPadrao) {
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? TAMANHO_PADRAO : tamanho;
        if (p < 0) {
            throw new FiltroInvalidoException("A página deve ser 0 ou maior.");
        }
        if (t < 1 || t > TAMANHO_MAXIMO) {
            throw new FiltroInvalidoException("O tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO + ".");
        }
        return new Paginacao(p, t, orderBy(ordenacao == null ? ordenacaoPadrao : ordenacao, colunasPermitidas));
    }

    /** Página e tamanho validados, para listagens com ordenação fixa. */
    public static Paginacao de(Integer pagina, Integer tamanho) {
        Paginacao validada = de(pagina, tamanho, "fixa,asc", Map.of("fixa", "1"), "fixa,asc");
        return new Paginacao(validada.pagina(), validada.tamanho(), null);
    }

    public long deslocamento() {
        return (long) pagina * tamanho;
    }

    private static String orderBy(String ordenacao, Map<String, String> colunasPermitidas) {
        String[] partes = ordenacao.split(",");
        String coluna = partes.length == 2 ? colunasPermitidas.get(partes[0].strip()) : null;
        String direcao = partes.length == 2 ? partes[1].strip().toLowerCase() : "";
        if (coluna == null || !(direcao.equals("asc") || direcao.equals("desc"))) {
            throw new FiltroInvalidoException("Ordenação inválida: use campo,asc ou campo,desc com um dos campos "
                    + String.join(", ", colunasPermitidas.keySet()) + ".");
        }
        return coluna + " " + direcao.toUpperCase() + " NULLS LAST";
    }
}
