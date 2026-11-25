package br.edu.ifrs.salaprecisa.api;

import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados recebidos pela API para cadastrar ou alterar uma sala.
 */
public record SalaRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotNull(message = "O tipo é obrigatório.")
        TipoSala tipo,

        @NotNull(message = "A capacidade é obrigatória.")
        @Min(value = 1, message = "A capacidade deve ser de no mínimo 1.")
        @Max(value = SalaService.CAPACIDADE_MAXIMA, message = "A capacidade deve ser de no máximo 1000.")
        Integer capacidade) {
}
