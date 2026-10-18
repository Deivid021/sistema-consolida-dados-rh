package br.edu.unidavi.rhgravatai.repository;

import br.edu.unidavi.rhgravatai.exception.CargaEmAndamentoException;
import br.edu.unidavi.rhgravatai.model.OrigemCarga;
import br.edu.unidavi.rhgravatai.model.OrigemTeto;
import br.edu.unidavi.rhgravatai.model.StatusCarga;
import br.edu.unidavi.rhgravatai.model.Carga;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CargaRepository {

    private final JdbcClient jdbc;

    public CargaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Cria uma carga EM_ANDAMENTO. O índice único parcial garante uma carga por vez, mesmo
     * com disparos simultâneos.
     */
    public Carga criar(OrigemCarga origem, LocalDate desligamentoInicio, LocalDate desligamentoFim) {
        try {
            long id = jdbc.sql("""
                            INSERT INTO carga (origem, status, desligamento_inicio, desligamento_fim)
                            VALUES (:origem, 'EM_ANDAMENTO', :inicio, :fim)
                            RETURNING id""")
                    .param("origem", origem.name())
                    .param("inicio", desligamentoInicio)
                    .param("fim", desligamentoFim)
                    .query(Long.class)
                    .single();
            return buscar(id).orElseThrow();
        } catch (DuplicateKeyException e) {
            throw new CargaEmAndamentoException();
        }
    }

    public Optional<Carga> buscar(long id) {
        return jdbc.sql("SELECT * FROM carga WHERE id = :id")
                .param("id", id)
                .query(CargaRepository::mapear)
                .optional();
    }

    public Optional<Carga> ultimaComSucesso() {
        return jdbc.sql("SELECT * FROM carga WHERE status = 'SUCESSO' ORDER BY fim DESC LIMIT 1")
                .query(CargaRepository::mapear)
                .optional();
    }

    /** Fim da última carga bem-sucedida, exibido em todas as telas (FR-013); nulo antes da primeira. */
    public OffsetDateTime ultimaAtualizacao() {
        return jdbc.sql("SELECT fim FROM carga WHERE status = 'SUCESSO' ORDER BY fim DESC LIMIT 1")
                .query(OffsetDateTime.class)
                .optional()
                .orElse(null);
    }

    public List<Carga> listar(int pagina, int tamanho) {
        return jdbc.sql("SELECT * FROM carga ORDER BY inicio DESC, id DESC LIMIT :limite OFFSET :deslocamento")
                .param("limite", tamanho)
                .param("deslocamento", (long) pagina * tamanho)
                .query(CargaRepository::mapear)
                .list();
    }

    public long contar() {
        return jdbc.sql("SELECT count(*) FROM carga").query(Long.class).single();
    }

    public void registrarTotais(long id, int totalInformado, int totalPaginas) {
        jdbc.sql("UPDATE carga SET total_informado = :total, total_paginas = :paginas WHERE id = :id")
                .param("total", totalInformado)
                .param("paginas", totalPaginas)
                .param("id", id)
                .update();
    }

    public void registrarPaginaLida(long id, int paginasLidas, int rejeitadosNaPagina, Integer totalInformadoAtual) {
        jdbc.sql("""
                        UPDATE carga
                           SET paginas_lidas = :paginas,
                               rejeitados = rejeitados + :rejeitados,
                               total_informado_final = :totalAtual
                         WHERE id = :id""")
                .param("paginas", paginasLidas)
                .param("rejeitados", rejeitadosNaPagina)
                .param("totalAtual", totalInformadoAtual)
                .param("id", id)
                .update();
    }

    public void registrarGravados(long id, int gravados) {
        jdbc.sql("UPDATE carga SET gravados = :gravados WHERE id = :id")
                .param("gravados", gravados)
                .param("id", id)
                .update();
    }

    public void registrarTeto(long id, BigDecimal valor, OrigemTeto origem) {
        jdbc.sql("UPDATE carga SET teto_valor = :valor, teto_origem = :origem WHERE id = :id")
                .param("valor", valor)
                .param("origem", origem.name())
                .param("id", id)
                .update();
    }

    public void concluirComSucesso(long id, int novos, int alterados, int naoEncontrados) {
        jdbc.sql("""
                        UPDATE carga
                           SET status = 'SUCESSO', fim = now(), novos = :novos,
                               alterados = :alterados, nao_encontrados = :naoEncontrados
                         WHERE id = :id""")
                .param("novos", novos)
                .param("alterados", alterados)
                .param("naoEncontrados", naoEncontrados)
                .param("id", id)
                .update();
    }

    /** Encerra a carga como FALHA ou INCONSISTENTE, sem alterar os dados publicados. */
    public void encerrarSemPublicar(long id, StatusCarga status, String mensagem) {
        jdbc.sql("UPDATE carga SET status = :status, fim = now(), mensagem = :mensagem WHERE id = :id")
                .param("status", status.name())
                .param("mensagem", mensagem)
                .param("id", id)
                .update();
    }

    /** Marca como FALHA as cargas que ficaram EM_ANDAMENTO após um reinício da aplicação. */
    public int encerrarInterrompidas() {
        return jdbc.sql("""
                        UPDATE carga SET status = 'FALHA', fim = now(), mensagem = 'interrompida'
                         WHERE status = 'EM_ANDAMENTO'""")
                .update();
    }

    private static Carga mapear(ResultSet rs, int linha) throws SQLException {
        String tetoOrigem = rs.getString("teto_origem");
        return new Carga(
                rs.getLong("id"),
                OrigemCarga.valueOf(rs.getString("origem")),
                StatusCarga.valueOf(rs.getString("status")),
                rs.getObject("inicio", OffsetDateTime.class),
                rs.getObject("fim", OffsetDateTime.class),
                rs.getObject("desligamento_inicio", LocalDate.class),
                rs.getObject("desligamento_fim", LocalDate.class),
                (Integer) rs.getObject("total_informado"),
                (Integer) rs.getObject("total_informado_final"),
                (Integer) rs.getObject("total_paginas"),
                rs.getInt("paginas_lidas"),
                rs.getInt("gravados"),
                rs.getInt("rejeitados"),
                (Integer) rs.getObject("novos"),
                (Integer) rs.getObject("alterados"),
                (Integer) rs.getObject("nao_encontrados"),
                rs.getBigDecimal("teto_valor"),
                tetoOrigem == null ? null : OrigemTeto.valueOf(tetoOrigem),
                rs.getString("mensagem"));
    }
}
