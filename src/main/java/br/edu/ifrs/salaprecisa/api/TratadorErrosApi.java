package br.edu.ifrs.salaprecisa.api;

import br.edu.ifrs.salaprecisa.excecao.ConflitoReservaException;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte as exceções lançadas pelos controllers da API em respostas HTTP padronizadas.
 * Mensagens internas (stack trace, SQL, nomes de classes) nunca são devolvidas ao cliente.
 */
@RestControllerAdvice(basePackageClasses = TratadorErrosApi.class)
public class TratadorErrosApi {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorErrosApi.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflitoReservaException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoReservaException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos.", detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado.", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName() + ".", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception ex) {
        if (ex instanceof ErrorResponse erroHttp) {
            // Erros do próprio Spring MVC (ex.: 415 tipo de conteúdo não suportado) já informam
            // o status HTTP correto; não são falhas internas e não devem gerar stack trace no log.
            int status = erroHttp.getStatusCode().value();
            return ResponseEntity.status(status)
                    .body(new ErroResponse(status, "Requisição não suportada.", List.of()));
        }
        LOG.error("Erro inesperado ao processar requisição da API", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), mensagem, detalhes));
    }
}
