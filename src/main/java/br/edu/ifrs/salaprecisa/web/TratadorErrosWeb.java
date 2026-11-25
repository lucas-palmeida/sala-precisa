package br.edu.ifrs.salaprecisa.web;

import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Exibe uma página de erro amigável quando as telas recebem identificadores inválidos
 * ou inexistentes.
 */
@ControllerAdvice(basePackageClasses = TratadorErrosWeb.class)
public class TratadorErrosWeb {

    private static final String VIEW_ERRO = "erro";

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RecursoNaoEncontradoException ex, Model model) {
        model.addAttribute("mensagem", ex.getMessage());
        return VIEW_ERRO;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String parametroInvalido(Model model) {
        model.addAttribute("mensagem", "Endereço inválido.");
        return VIEW_ERRO;
    }
}
