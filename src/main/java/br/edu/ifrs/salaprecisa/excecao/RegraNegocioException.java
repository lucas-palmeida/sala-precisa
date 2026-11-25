package br.edu.ifrs.salaprecisa.excecao;

/**
 * Lançada quando uma operação viola uma regra de negócio
 * (por exemplo: período inválido, sala inativa, nome duplicado).
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
