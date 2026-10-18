package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.OpcaoFiltro;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.model.Situacao;
import br.edu.unidavi.rhgravatai.model.Vinculo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acesso à tabela vinculo: consulta (RF02), totais e publicação das cargas (RF01). */
@Repository
public class VinculoRepository {

    /** Campos de ordenação do contrato e suas colunas. */
    public static final Map<String, String> ORDENACOES = ordenacoes();

    /** Colunas filtráveis que têm lista de opções (FR-016). */
    public enum ColunaOpcao { SITUACAO, REGIME, CLASSIFICACAO, SECRETARIA, LOCAL_TRABALHO, CARGO }

    /** Colunas copiadas da área de preparação na publicação. */
    private static final List<String> COLUNAS_PUBLICADAS = List.of(
            "entidade", "matricula", "nome", "situacao", "data_admissao", "data_desligamento",
            "motivo_desligamento", "regime", "classificacao", "forma_investidura", "secretaria",
            "local_trabalho", "cargo", "funcao", "nivel", "salario_base", "horario",
            "horas_mensais_min", "horas_semanais_min", "valores_originais", "texto_busca");

    /** Colunas lidas por {@link #mapear}. */
    private static final List<String> COLUNAS_LIDAS = List.of(
            "id", "entidade", "matricula", "nome", "situacao", "data_admissao", "data_desligamento",
            "motivo_desligamento", "regime", "classificacao", "forma_investidura", "secretaria",
            "local_trabalho", "cargo", "funcao", "nivel", "salario_base", "horario", "horas_mensais_min",
            "horas_semanais_min", "ativo_na_fonte");

    private static final String COLUNAS = colunas("");

    private final JdbcClient jdbc;

    public VinculoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---- Consulta (RF02) ----

    public List<Vinculo> buscar(FiltroVinculos filtro, Paginacao paginacao) {
        FiltroSql sql = FiltroSql.de(filtro, "");
        return jdbc.sql("SELECT " + COLUNAS + " FROM vinculo WHERE " + sql.where()
                        + " ORDER BY " + FiltroSql.orderBy(paginacao, ORDENACOES) + ", id"
                        + " LIMIT :limite OFFSET :deslocamento")
                .params(sql.com(Map.of("limite", paginacao.tamanho(), "deslocamento", paginacao.deslocamento())))
                .query(VinculoRepository::mapear)
                .list();
    }

    public long contar(FiltroVinculos filtro) {
        FiltroSql sql = FiltroSql.de(filtro, "");
        return jdbc.sql("SELECT count(*) FROM vinculo WHERE " + sql.where())
                .params(sql.parametros()).query(Long.class).single();
    }

    /** Todos os vínculos do filtro, lidos aos poucos. Fechar o fluxo depois do uso. */
    public Stream<Vinculo> fluxo(FiltroVinculos filtro, Paginacao ordenacao) {
        FiltroSql sql = FiltroSql.de(filtro, "");
        return jdbc.sql("SELECT " + COLUNAS + " FROM vinculo WHERE " + sql.where()
                        + " ORDER BY " + FiltroSql.orderBy(ordenacao, ORDENACOES) + ", id")
                .params(sql.parametros())
                .withFetchSize(1000)
                .query(VinculoRepository::mapear)
                .stream();
    }

    public List<OpcaoFiltro> opcoes(ColunaOpcao opcao) {
        String coluna = opcao.name().toLowerCase();
        return jdbc.sql("SELECT " + coluna + " AS valor, count(*) AS quantidade FROM vinculo"
                        + " GROUP BY " + coluna + " ORDER BY " + coluna + " NULLS LAST")
                .query((rs, i) -> new OpcaoFiltro(rs.getString("valor"), rs.getLong("quantidade")))
                .list();
    }

    public long contarTodos() {
        return jdbc.sql("SELECT count(*) FROM vinculo").query(Long.class).single();
    }

    public long contarAtivos() {
        return jdbc.sql("SELECT count(*) FROM vinculo WHERE " + FiltroSql.ativo("")).query(Long.class).single();
    }

    /** Vínculo ativo do prefeito com salário, o mais recente se houver mais de um (FR-037). */
    public Optional<Vinculo> buscarPrefeitoAtivo(String cargoPrefeito) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM vinculo WHERE cargo = :cargo AND salario_base > 0 AND "
                        + FiltroSql.ativo("") + " ORDER BY data_admissao DESC NULLS LAST, id DESC LIMIT 1")
                .param("cargo", cargoPrefeito)
                .query(VinculoRepository::mapear)
                .optional();
    }

    // ---- Publicação de uma carga (FR-008, FR-011) ----

    public boolean existeAlgum() {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM vinculo)").query(Boolean.class).single();
    }

    /** Vínculos da área de preparação que ainda não existem. */
    public int contarNovosNaPreparacao(long cargaId) {
        return jdbc.sql("""
                        SELECT count(*) FROM vinculo_staging s
                         WHERE s.carga_id = :carga
                           AND NOT EXISTS (SELECT 1 FROM vinculo v
                                            WHERE v.entidade = s.entidade AND v.matricula = s.matricula)""")
                .param("carga", cargaId).query(Integer.class).single();
    }

    /** Insere os novos e atualiza os existentes a partir da área de preparação. */
    public void publicarPreparacao(long cargaId) {
        String colunas = String.join(", ", COLUNAS_PUBLICADAS);
        String atualizacoes = String.join(", ", COLUNAS_PUBLICADAS.stream()
                .filter(c -> !c.equals("entidade") && !c.equals("matricula"))
                .map(c -> c + " = EXCLUDED." + c)
                .toList());
        jdbc.sql("""
                        INSERT INTO vinculo (%s, ativo_na_fonte, primeira_carga_id, ultima_carga_id, visto_por_ultimo_em)
                        SELECT %s, true, :carga, :carga, now()
                          FROM vinculo_staging
                         WHERE carga_id = :carga
                        ON CONFLICT (entidade, matricula) DO UPDATE SET
                            %s,
                            ativo_na_fonte = true,
                            ultima_carga_id = EXCLUDED.ultima_carga_id,
                            visto_por_ultimo_em = EXCLUDED.visto_por_ultimo_em""".formatted(colunas, colunas, atualizacoes))
                .param("carga", cargaId)
                .update();
    }

    /** Quem não veio nesta carga é mantido e marcado como não encontrado; nunca é apagado. */
    public void marcarAusentes(long cargaId) {
        jdbc.sql("UPDATE vinculo SET ativo_na_fonte = false WHERE ativo_na_fonte AND ultima_carga_id <> :carga")
                .param("carga", cargaId)
                .update();
    }

    public int contarAusentes() {
        return jdbc.sql("SELECT count(*) FROM vinculo WHERE NOT ativo_na_fonte").query(Integer.class).single();
    }

    /** Colunas lidas por {@link #mapear}, com o alias da tabela (ex.: "v."). */
    static String colunas(String prefixo) {
        return String.join(", ", COLUNAS_LIDAS.stream().map(c -> prefixo + c).toList());
    }

    static Vinculo mapear(ResultSet rs, int linha) throws SQLException {
        return new Vinculo(
                rs.getLong("id"),
                rs.getString("entidade"),
                rs.getString("matricula"),
                rs.getString("nome"),
                Situacao.valueOf(rs.getString("situacao")),
                rs.getObject("data_admissao", LocalDate.class),
                rs.getObject("data_desligamento", LocalDate.class),
                rs.getString("motivo_desligamento"),
                rs.getString("regime"),
                rs.getString("classificacao"),
                rs.getString("forma_investidura"),
                rs.getString("secretaria"),
                rs.getString("local_trabalho"),
                rs.getString("cargo"),
                rs.getString("funcao"),
                rs.getString("nivel"),
                rs.getBigDecimal("salario_base"),
                rs.getString("horario"),
                (Integer) rs.getObject("horas_mensais_min"),
                (Integer) rs.getObject("horas_semanais_min"),
                rs.getBoolean("ativo_na_fonte"));
    }

    private static Map<String, String> ordenacoes() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("nome", "nome");
        mapa.put("matricula", "matricula");
        mapa.put("situacao", "situacao");
        mapa.put("regime", "regime");
        mapa.put("secretaria", "secretaria");
        mapa.put("cargo", "cargo");
        mapa.put("nivel", "nivel");
        mapa.put("salarioBase", "salario_base");
        mapa.put("dataAdmissao", "data_admissao");
        mapa.put("dataDesligamento", "data_desligamento");
        return Collections.unmodifiableMap(mapa);
    }
}
