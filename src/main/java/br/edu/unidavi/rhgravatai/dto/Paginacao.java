package br.edu.unidavi.rhgravatai.dto;

import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import java.util.Set;

/**
 * Página, tamanho e ordenação pedidos pelo usuário, já validados. A ordenação guarda o nome do
 * campo do contrato (ex.: {@code salarioBase}); a coluna correspondente é decidida no
 * repositório, para nenhum texto do usuário chegar ao SQL.
 *
 * @param campo nulo quando a listagem tem ordenação fixa
 */
public record Paginacao(int pagina, int tamanho, String campo, boolean crescente) {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    /**
     * @param ordenacao "campo,asc" ou "campo,desc"; nulo usa {@code padrao}
     * @param camposPermitidos campos de ordenação aceitos pelo endpoint
     */
    public static Paginacao de(Integer pagina, Integer tamanho, String ordenacao,
                               Set<String> camposPermitidos, String padrao) {
        Paginacao base = de(pagina, tamanho);
        String[] partes = (ordenacao == null ? padrao : ordenacao).split(",");
        String campo = partes.length == 2 ? partes[0].strip() : "";
        String direcao = partes.length == 2 ? partes[1].strip().toLowerCase() : "";
        if (!camposPermitidos.contains(campo) || !(direcao.equals("asc") || direcao.equals("desc"))) {
            throw new FiltroInvalidoException("Ordenação inválida: use campo,asc ou campo,desc com um dos campos "
                    + String.join(", ", camposPermitidos) + ".");
        }
        return new Paginacao(base.pagina(), base.tamanho(), campo, direcao.equals("asc"));
    }

    /** Página e tamanho validados, para listagens com ordenação fixa. */
    public static Paginacao de(Integer pagina, Integer tamanho) {
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? TAMANHO_PADRAO : tamanho;
        if (p < 0) {
            throw new FiltroInvalidoException("A página deve ser 0 ou maior.");
        }
        if (t < 1 || t > TAMANHO_MAXIMO) {
            throw new FiltroInvalidoException("O tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO + ".");
        }
        return new Paginacao(p, t, null, true);
    }

    public long deslocamento() {
        return (long) pagina * tamanho;
    }
}
