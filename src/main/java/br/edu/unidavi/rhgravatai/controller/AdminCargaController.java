package br.edu.unidavi.rhgravatai.controller;

import br.edu.unidavi.rhgravatai.dto.ListaCargasResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.exception.AcessoNegadoException;
import br.edu.unidavi.rhgravatai.model.OrigemCarga;
import br.edu.unidavi.rhgravatai.model.Carga;
import br.edu.unidavi.rhgravatai.service.CargaService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    private final CargaService cargaService;
    private final Executor tarefas;
    private final byte[] token;

    public AdminCargaController(CargaService cargaService,
                                @Qualifier("applicationTaskExecutor") Executor tarefas,
                                @Value("${admin.token:}") String token) {
        this.cargaService = cargaService;
        this.tarefas = tarefas;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Carga disparar(@RequestHeader(name = "X-Admin-Token", required = false) String tokenRecebido) {
        verificarToken(tokenRecebido);
        Carga carga = cargaService.iniciar(OrigemCarga.MANUAL);
        tarefas.execute(() -> cargaService.executar(carga.id()));
        return carga;
    }

    @GetMapping
    public ListaCargasResposta listar(@RequestHeader(name = "X-Admin-Token", required = false) String tokenRecebido,
                                      @RequestParam(required = false) Integer pagina,
                                      @RequestParam(required = false) Integer tamanho) {
        verificarToken(tokenRecebido);
        Paginacao paginacao = Paginacao.de(pagina, tamanho);
        return cargaService.listar(paginacao.pagina(), paginacao.tamanho());
    }

    private void verificarToken(String tokenRecebido) {
        boolean valido = token.length > 0 && tokenRecebido != null
                && MessageDigest.isEqual(token, tokenRecebido.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            throw new AcessoNegadoException();
        }
    }
}
