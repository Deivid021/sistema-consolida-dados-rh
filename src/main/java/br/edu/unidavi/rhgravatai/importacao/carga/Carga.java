package br.edu.unidavi.rhgravatai.importacao.carga;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Uma execução de importação (FR-009). */
public record Carga(
        long id,
        OrigemCarga origem,
        StatusCarga status,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        LocalDate desligamentoInicio,
        LocalDate desligamentoFim,
        Integer totalInformado,
        Integer totalInformadoFinal,
        Integer totalPaginas,
        int paginasLidas,
        int gravados,
        int rejeitados,
        Integer novos,
        Integer alterados,
        Integer naoEncontrados,
        BigDecimal tetoValor,
        OrigemTeto tetoOrigem,
        String mensagem) {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Data de início no horário de Brasília (o banco devolve o instante em UTC). */
    public LocalDate dataInicio() {
        return inicio.atZoneSameInstant(FUSO).toLocalDate();
    }

    public enum StatusCarga { EM_ANDAMENTO, SUCESSO, FALHA, INCONSISTENTE }

    public enum OrigemCarga { AGENDADA, MANUAL }

    public enum OrigemTeto { FONTE, MANUAL, SUSPENSA }
}
