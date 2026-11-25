package br.edu.ifrs.salaprecisa.dominio;

/**
 * Situações possíveis de uma reserva.
 *
 * <ul>
 *   <li>ATIVA: reserva vigente (agendada ou em andamento);</li>
 *   <li>CANCELADA: cancelada pelo usuário antes do fim;</li>
 *   <li>ENCERRADA: o período terminou e a sala foi liberada automaticamente.</li>
 * </ul>
 */
public enum StatusReserva {

    ATIVA("Ativa"),
    CANCELADA("Cancelada"),
    ENCERRADA("Encerrada");

    private final String descricao;

    StatusReserva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
