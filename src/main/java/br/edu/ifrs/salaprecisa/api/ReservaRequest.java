package br.edu.ifrs.salaprecisa.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Dados recebidos pela API para criar uma reserva.
 */
public record ReservaRequest(
        @NotNull(message = "A sala é obrigatória.")
        Long salaId,

        @NotBlank(message = "O solicitante é obrigatório.")
        @Size(max = 100, message = "O solicitante deve ter no máximo 100 caracteres.")
        String solicitante,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDateTime inicio,

        @NotNull(message = "A data de fim é obrigatória.")
        LocalDateTime fim) {
}
