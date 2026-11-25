package br.edu.ifrs.salaprecisa.excecao;

/**
 * Lançada quando se tenta reservar uma sala em um período que se sobrepõe
 * a outra reserva ativa da mesma sala.
 */
public class ConflitoReservaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflitoReservaException(String mensagem) {
        super(mensagem);
    }
}
