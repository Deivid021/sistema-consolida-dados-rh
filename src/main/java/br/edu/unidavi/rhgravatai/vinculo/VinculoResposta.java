package br.edu.unidavi.rhgravatai.vinculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Vínculo como aparece na API (esquema Vinculo do contrato; FR-020). */
public record VinculoResposta(
        String matricula,
        String nome,
        Situacao situacao,
        String regime,
        String classificacao,
        String secretaria,
        String localTrabalho,
        String cargo,
        String funcao,
        String nivel,
        BigDecimal salarioBase,
        BigDecimal horasMensais,
        LocalDate dataAdmissao,
        LocalDate dataDesligamento,
        String motivoDesligamento,
        boolean ativoNaFonte) {

    /** Colunas lidas por {@link #mapear}; o prefixo é o alias da tabela vinculo. */
    public static String colunas(String prefixo) {
        return String.join(", ",
                prefixo + "matricula", prefixo + "nome", prefixo + "situacao", prefixo + "regime",
                prefixo + "classificacao", prefixo + "secretaria", prefixo + "local_trabalho", prefixo + "cargo",
                prefixo + "funcao", prefixo + "nivel", prefixo + "salario_base", prefixo + "horas_mensais_min",
                prefixo + "data_admissao", prefixo + "data_desligamento", prefixo + "motivo_desligamento",
                prefixo + "ativo_na_fonte");
    }

    public static VinculoResposta mapear(ResultSet rs, int linha) throws SQLException {
        Integer minutos = (Integer) rs.getObject("horas_mensais_min");
        return new VinculoResposta(
                rs.getString("matricula"),
                rs.getString("nome"),
                Situacao.valueOf(rs.getString("situacao")),
                rs.getString("regime"),
                rs.getString("classificacao"),
                rs.getString("secretaria"),
                rs.getString("local_trabalho"),
                rs.getString("cargo"),
                rs.getString("funcao"),
                rs.getString("nivel"),
                rs.getBigDecimal("salario_base"),
                minutos == null ? null : BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP),
                rs.getObject("data_admissao", LocalDate.class),
                rs.getObject("data_desligamento", LocalDate.class),
                rs.getString("motivo_desligamento"),
                rs.getBoolean("ativo_na_fonte"));
    }
}
