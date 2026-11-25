package br.edu.ifrs.salaprecisa.dominio;

import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/**
 * Reserva de uma sala por um solicitante durante um período [inicio, fim).
 *
 * <p>O período é tratado como intervalo semiaberto: a reserva ocupa a sala a partir
 * de {@code inicio} (inclusive) até {@code fim} (exclusive). Assim, uma reserva que
 * termina às 10h e outra que começa às 10h não são consideradas conflitantes.</p>
 */
@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(nullable = false, length = 100)
    private String solicitante;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status = StatusReserva.ATIVA;

    /** Construtor exigido pela JPA. */
    protected Reserva() {
    }

    public Reserva(Sala sala, String solicitante, LocalDateTime inicio, LocalDateTime fim) {
        this.sala = sala;
        this.solicitante = solicitante;
        this.inicio = inicio;
        this.fim = fim;
    }

    /**
     * Indica se o período desta reserva se sobrepõe ao período informado.
     * Períodos apenas adjacentes (um termina quando o outro começa) não se sobrepõem.
     */
    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return inicio.isBefore(outroFim) && outroInicio.isBefore(fim);
    }

    /** Indica se a reserva está ativa e ocupando a sala no instante informado. */
    public boolean estaEmAndamento(LocalDateTime agora) {
        return status == StatusReserva.ATIVA
                && !inicio.isAfter(agora)
                && agora.isBefore(fim);
    }

    /** Indica se a reserva está ativa, mas o período já terminou no instante informado. */
    public boolean deveSerEncerrada(LocalDateTime agora) {
        return status == StatusReserva.ATIVA && !fim.isAfter(agora);
    }

    /** Cancela a reserva. Somente reservas ativas podem ser canceladas. */
    public void cancelar() {
        if (status != StatusReserva.ATIVA) {
            throw new RegraNegocioException("Somente reservas ativas podem ser canceladas.");
        }
        this.status = StatusReserva.CANCELADA;
    }

    /** Encerra a reserva ao final do período, liberando a sala. */
    public void encerrar() {
        if (status != StatusReserva.ATIVA) {
            throw new RegraNegocioException("Somente reservas ativas podem ser encerradas.");
        }
        this.status = StatusReserva.ENCERRADA;
    }

    public Long getId() {
        return id;
    }

    public Sala getSala() {
        return sala;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public StatusReserva getStatus() {
        return status;
    }
}
