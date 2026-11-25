package br.edu.ifrs.salaprecisa.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Objeto de formulário (tela) para criação de reserva.
 * O formato de data corresponde ao do campo HTML {@code datetime-local}.
 *
 * <p>É intencionalmente uma classe de dados: o Spring MVC preenche os campos pelos
 * setters e o Thymeleaf os lê pelos getters (supressão justificada em docs/analise-estatica.md).</p>
 */
@SuppressWarnings("PMD.DataClass")
public class ReservaForm {

    private static final String FORMATO_DATA_HORA = "yyyy-MM-dd'T'HH:mm";

    @NotNull(message = "Selecione a sala.")
    private Long salaId;

    @NotBlank(message = "Informe o solicitante.")
    @Size(max = 100, message = "O solicitante deve ter no máximo 100 caracteres.")
    private String solicitante;

    @NotNull(message = "Informe a data e hora de início.")
    @DateTimeFormat(pattern = FORMATO_DATA_HORA)
    private LocalDateTime inicio;

    @NotNull(message = "Informe a data e hora de fim.")
    @DateTimeFormat(pattern = FORMATO_DATA_HORA)
    private LocalDateTime fim;

    public Long getSalaId() {
        return salaId;
    }

    public void setSalaId(Long salaId) {
        this.salaId = salaId;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }
}
