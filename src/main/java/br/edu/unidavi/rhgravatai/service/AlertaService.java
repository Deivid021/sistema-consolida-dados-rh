package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.config.PropriedadesAlertas;
import br.edu.unidavi.rhgravatai.dto.FiltroVinculos.CondicaoSql;
import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.VinculoResposta;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import br.edu.unidavi.rhgravatai.service.TetoVigente.Teto;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listagem dos alertas calculados na última carga (FR-040 a FR-042). */
@Service
public class AlertaService {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final String ORDEM = "abs(a.diferenca_percentual) DESC, v.nome NULLS LAST, v.id";

    private final JdbcClient jdbc;
    private final TetoVigente tetoVigente;
    private final UltimaAtualizacao ultimaAtualizacao;
    private final PropriedadesAlertas propriedades;

    public AlertaService(JdbcClient jdbc, TetoVigente tetoVigente, UltimaAtualizacao ultimaAtualizacao,
                         PropriedadesAlertas propriedades) {
        this.jdbc = jdbc;
        this.tetoVigente = tetoVigente;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.propriedades = propriedades;
    }

    @Transactional(readOnly = true)
    public PaginaAlertas listar(RegraAlerta regra, FiltroVinculos filtro, Paginacao paginacao) {
        Consulta consulta = consulta(regra, filtro);
        long total = jdbc.sql("SELECT count(*) FROM alerta a JOIN vinculo v ON v.id = a.vinculo_id WHERE " + consulta.where)
                .params(consulta.parametros).query(Long.class).single();
        List<Alerta> itens = jdbc.sql(consulta.select() + " ORDER BY " + ORDEM + " LIMIT :limite OFFSET :deslocamento")
                .params(consulta.parametros)
                .param("limite", paginacao.tamanho())
                .param("deslocamento", paginacao.deslocamento())
                .query(AlertaService::mapear)
                .list();
        return new PaginaAlertas(itens, total, paginacao.pagina(), paginacao.tamanho(), tetoVigente.obter(),
                propriedades.foraPadrao().percentual(), ultimaAtualizacao.obter());
    }

    public long contar(RegraAlerta regra, FiltroVinculos filtro) {
        Consulta consulta = consulta(regra, filtro);
        return jdbc.sql("SELECT count(*) FROM alerta a JOIN vinculo v ON v.id = a.vinculo_id WHERE " + consulta.where)
                .params(consulta.parametros).query(Long.class).single();
    }

    /** Todos os alertas do filtro, em fluxo, para a exportação. Consumir dentro de uma transação. */
    public Stream<Alerta> fluxo(RegraAlerta regra, FiltroVinculos filtro) {
        Consulta consulta = consulta(regra, filtro);
        return jdbc.sql(consulta.select() + " ORDER BY " + ORDEM)
                .params(consulta.parametros)
                .withFetchSize(1000)
                .query(AlertaService::mapear)
                .stream();
    }

    private static Consulta consulta(RegraAlerta regra, FiltroVinculos filtro) {
        CondicaoSql condicao = filtro.paraSql("v.");
        Map<String, Object> parametros = new HashMap<>(condicao.parametros());
        String where = condicao.where();
        if (regra != null) {
            where += " AND a.regra = :regra";
            parametros.put("regra", regra.name());
        }
        return new Consulta(where, parametros);
    }

    private static Alerta mapear(ResultSet rs, int linha) throws SQLException {
        RegraAlerta regra = RegraAlerta.valueOf(rs.getString("regra"));
        BigDecimal valorVinculo = rs.getBigDecimal("valor_vinculo");
        BigDecimal referencia = rs.getBigDecimal("valor_referencia");
        BigDecimal diferenca = rs.getBigDecimal("diferenca");
        BigDecimal percentual = rs.getBigDecimal("diferenca_percentual");
        Integer grupo = (Integer) rs.getObject("grupo_tamanho");
        return new Alerta(regra, VinculoResposta.mapear(rs, linha), valorVinculo, referencia, diferenca, percentual,
                grupo, descrever(regra, referencia, diferenca, percentual, grupo));
    }

    /** Texto do critério, exibido junto ao aviso de que é sinalização para verificação (FR-041). */
    static String descrever(RegraAlerta regra, BigDecimal referencia, BigDecimal diferenca, BigDecimal percentual,
                            Integer grupo) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(BRASIL);
        String texto;
        if (regra == RegraAlerta.TETO) {
            texto = "Salário base " + moeda.format(diferenca) + " acima do teto municipal (subsídio do prefeito) de "
                    + moeda.format(referencia) + ".";
        } else {
            NumberFormat numero = NumberFormat.getNumberInstance(BRASIL);
            numero.setMinimumFractionDigits(1);
            numero.setMaximumFractionDigits(1);
            String direcao = percentual.signum() >= 0 ? "acima" : "abaixo";
            texto = "Salário base " + numero.format(percentual.abs()) + "% " + direcao + " da mediana ("
                    + moeda.format(referencia) + ") dos " + grupo
                    + " vínculos ativos com o mesmo cargo, nível e carga horária.";
        }
        // O formato de moeda do Java usa espaço não separável depois de "R$"
        return texto.replace('\u00A0', ' ');
    }

    private record Consulta(String where, Map<String, Object> parametros) {

        String select() {
            return "SELECT a.regra, a.valor_vinculo, a.valor_referencia, a.diferenca, a.diferenca_percentual,"
                    + " a.grupo_tamanho, " + VinculoResposta.colunas("v.")
                    + " FROM alerta a JOIN vinculo v ON v.id = a.vinculo_id WHERE " + where;
        }
    }

    public record Alerta(
            RegraAlerta regra,
            VinculoResposta vinculo,
            BigDecimal valorVinculo,
            BigDecimal valorReferencia,
            BigDecimal diferenca,
            BigDecimal diferencaPercentual,
            Integer grupoTamanho,
            String descricaoRegra) {
    }

    public record PaginaAlertas(
            List<Alerta> itens,
            long total,
            int pagina,
            int tamanho,
            Teto teto,
            BigDecimal percentualForaPadrao,
            OffsetDateTime ultimaAtualizacao) {
    }
}
