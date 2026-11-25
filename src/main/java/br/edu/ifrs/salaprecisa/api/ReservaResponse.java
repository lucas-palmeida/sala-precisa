package br.edu.ifrs.salaprecisa.api;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import java.time.LocalDateTime;

/**
 * Representação de uma reserva devolvida pela API.
 */
public record ReservaResponse(Long id, Long salaId, String salaNome, String solicitante,
                              LocalDateTime inicio, LocalDateTime fim, StatusReserva status) {

    public static ReservaResponse de(Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getSala().getId(), reserva.getSala().getNome(),
                reserva.getSolicitante(), reserva.getInicio(), reserva.getFim(), reserva.getStatus());
    }
}
