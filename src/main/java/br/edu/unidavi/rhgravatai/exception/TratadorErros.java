package br.edu.unidavi.rhgravatai.exception;

import br.edu.unidavi.rhgravatai.model.Carga;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte exceções em respostas application/problem+json com mensagem em português.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TratadorErros {

    @ExceptionHandler(FiltroInvalidoException.class)
    ProblemDetail filtroInvalido(FiltroInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "Filtro inválido", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Filtro inválido",
                "Valor inválido para o parâmetro '" + e.getName() + "'.");
    }

    @ExceptionHandler(BindException.class)
    ProblemDetail vinculacaoInvalida(BindException e) {
        String campo = e.getFieldError() != null ? e.getFieldError().getField() : "desconhecido";
        return problema(HttpStatus.BAD_REQUEST, "Filtro inválido",
                "Valor inválido para o parâmetro '" + campo + "'.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail parametroAusente(MissingServletRequestParameterException e) {
        return problema(HttpStatus.BAD_REQUEST, "Filtro inválido",
                "O parâmetro '" + e.getParameterName() + "' é obrigatório.");
    }

    @ExceptionHandler(ExportacaoMuitoGrandeException.class)
    ProblemDetail exportacaoMuitoGrande(ExportacaoMuitoGrandeException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Relatório muito grande para PDF", e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail acessoNegado(AcessoNegadoException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Acesso negado", e.getMessage());
    }

    @ExceptionHandler(CargaEmAndamentoException.class)
    ProblemDetail cargaEmAndamento(CargaEmAndamentoException e) {
        return problema(HttpStatus.CONFLICT, "Carga em andamento", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
