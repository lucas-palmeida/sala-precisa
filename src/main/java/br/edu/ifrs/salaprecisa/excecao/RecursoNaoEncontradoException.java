package br.edu.ifrs.salaprecisa.excecao;

/**
 * Lançada quando uma sala ou reserva procurada não existe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
