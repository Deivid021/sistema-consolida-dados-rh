package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.exception.AcessoNegadoException;
import br.edu.unidavi.rhgravatai.exception.FiltroInvalidoException;
import br.edu.unidavi.rhgravatai.model.Carga.OrigemCarga;
import br.edu.unidavi.rhgravatai.model.Carga;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import br.edu.unidavi.rhgravatai.service.ExecutorCarga;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operação da carga (RF01): disparo manual, para a carga inicial e demonstrações, e
 * acompanhamento. Protegido pelo cabeçalho X-Admin-Token.
 */
@RestController
@RequestMapping("/api/v1/admin/cargas")
public class AdminCargaController {

    private final ExecutorCarga executor;
    private final CargaRepository cargas;
    private final Executor tarefas;
    private final byte[] token;

    public AdminCargaController(ExecutorCarga executor, CargaRepository cargas,
                                @Qualifier("applicationTaskExecutor") Executor tarefas,
                                @Value("${admin.token:}") String token) {
        this.executor = executor;
        this.cargas = cargas;
        this.tarefas = tarefas;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Carga disparar(@RequestHeader(name = "X-Admin-Token", required = false) String tokenRecebido) {
        verificarToken(tokenRecebido);
        Carga carga = executor.iniciar(OrigemCarga.MANUAL);
        tarefas.execute(() -> executor.executar(carga.id()));
        return carga;
    }

    @GetMapping
    public ListaCargas listar(@RequestHeader(name = "X-Admin-Token", required = false) String tokenRecebido,
                              @RequestParam(defaultValue = "0") int pagina,
                              @RequestParam(defaultValue = "20") int tamanho) {
        verificarToken(tokenRecebido);
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new FiltroInvalidoException("Página deve ser 0 ou maior e tamanho entre 1 e 100.");
        }
        return new ListaCargas(cargas.listar(pagina, tamanho), cargas.contar());
    }

    private void verificarToken(String tokenRecebido) {
        boolean valido = token.length > 0 && tokenRecebido != null
                && MessageDigest.isEqual(token, tokenRecebido.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            throw new AcessoNegadoException();
        }
    }

    public record ListaCargas(List<Carga> itens, long total) {
    }
}
