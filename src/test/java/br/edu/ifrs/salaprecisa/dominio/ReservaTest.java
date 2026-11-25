package br.edu.ifrs.salaprecisa.dominio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes unitários da entidade {@link Reserva}: regras de sobreposição de períodos,
 * situação no tempo e transições de status.
 *
 * <p>Reserva base usada nos testes: 10/01/2027, das 10:00 às 12:00.</p>
 */
@DisplayName("Reserva (entidade)")
class ReservaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2027, 1, 10, 10, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2027, 1, 10, 12, 0);

    private Sala sala;
    private Reserva reserva;

    @BeforeEach
    void criarReserva() {
        sala = new Sala("Lab 101", TipoSala.LABORATORIO, 30);
        reserva = new Reserva(sala, "Ana", INICIO, FIM);
    }

    @Test
    @DisplayName("nova reserva guarda os dados informados e começa ATIVA")
    void novaReservaComecaAtiva() {
        assertAll(
                () -> assertSame(sala, reserva.getSala()),
                () -> assertEquals("Ana", reserva.getSolicitante()),
                () -> assertEquals(INICIO, reserva.getInicio()),
                () -> assertEquals(FIM, reserva.getFim()),
                () -> assertEquals(StatusReserva.ATIVA, reserva.getStatus()));
    }

    @Nested
    @DisplayName("sobrepoe(inicio, fim)")
    class Sobreposicao {

        /*
         * Partição de equivalência e análise de valor-limite sobre o intervalo [10:00, 12:00).
         * Os casos "adjacentes" (limite exato) não devem ser considerados sobreposição.
         */
        @ParameterizedTest(name = "[{0} – {1}) sobrepõe [10:00 – 12:00)? {2}")
        @CsvSource({
            "07:00, 08:00, false",   // totalmente antes
            "09:00, 10:00, false",   // adjacente antes (fim == início)
            "09:00, 10:01, true",    // invade 1 minuto no início
            "10:00, 12:00, true",    // período idêntico
            "10:30, 11:30, true",    // contido
            "09:00, 13:00, true",    // contém
            "11:59, 13:00, true",    // invade 1 minuto no fim
            "12:00, 13:00, false",   // adjacente depois (início == fim)
            "13:00, 14:00, false"    // totalmente depois
        })
        void verificaSobreposicao(String inicio, String fim, boolean esperado) {
            LocalDateTime outroInicio = LocalDateTime.parse("2027-01-10T" + inicio);
            LocalDateTime outroFim = LocalDateTime.parse("2027-01-10T" + fim);

            assertEquals(esperado, reserva.sobrepoe(outroInicio, outroFim));
        }
    }

    @Nested
    @DisplayName("estaEmAndamento(agora)")
    class EmAndamento {

        @ParameterizedTest(name = "às {0} está em andamento? {1}")
        @CsvSource({
            "09:59, false",  // antes do início
            "10:00, true",   // exatamente no início (inclusive)
            "11:00, true",   // no meio do período
            "11:59, true",   // último minuto
            "12:00, false",  // exatamente no fim (exclusive)
            "12:01, false"   // depois do fim
        })
        void verificaAndamentoNoTempo(String hora, boolean esperado) {
            assertEquals(esperado, reserva.estaEmAndamento(LocalDateTime.parse("2027-01-10T" + hora)));
        }

        @Test
        @DisplayName("reserva cancelada nunca está em andamento")
        void canceladaNaoEstaEmAndamento() {
            reserva.cancelar();

            assertFalse(reserva.estaEmAndamento(INICIO.plusMinutes(30)));
        }
    }

    @Nested
    @DisplayName("deveSerEncerrada(agora)")
    class DeveSerEncerrada {

        @ParameterizedTest(name = "às {0} deve ser encerrada? {1}")
        @CsvSource({
            "11:59, false",  // ainda em andamento
            "12:00, true",   // exatamente no fim
            "15:00, true"    // bem depois do fim
        })
        void verificaEncerramentoNoTempo(String hora, boolean esperado) {
            assertEquals(esperado, reserva.deveSerEncerrada(LocalDateTime.parse("2027-01-10T" + hora)));
        }

        @Test
        @DisplayName("reserva cancelada não precisa ser encerrada")
        void canceladaNaoDeveSerEncerrada() {
            reserva.cancelar();

            assertFalse(reserva.deveSerEncerrada(FIM.plusHours(1)));
        }
    }

    @Nested
    @DisplayName("cancelar()")
    class Cancelar {

        @Test
        @DisplayName("reserva ativa passa para CANCELADA")
        void cancelaReservaAtiva() {
            reserva.cancelar();

            assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
        }

        @Test
        @DisplayName("não permite cancelar duas vezes")
        void naoCancelaReservaJaCancelada() {
            reserva.cancelar();

            RegraNegocioException ex = assertThrows(RegraNegocioException.class, reserva::cancelar);
            assertEquals("Somente reservas ativas podem ser canceladas.", ex.getMessage());
        }

        @Test
        @DisplayName("não permite cancelar reserva encerrada")
        void naoCancelaReservaEncerrada() {
            reserva.encerrar();

            assertThrows(RegraNegocioException.class, reserva::cancelar);
            assertEquals(StatusReserva.ENCERRADA, reserva.getStatus());
        }
    }

    @Nested
    @DisplayName("encerrar()")
    class Encerrar {

        @Test
        @DisplayName("reserva ativa passa para ENCERRADA")
        void encerraReservaAtiva() {
            reserva.encerrar();

            assertEquals(StatusReserva.ENCERRADA, reserva.getStatus());
            assertFalse(reserva.estaEmAndamento(INICIO.plusMinutes(30)));
        }

        @Test
        @DisplayName("não permite encerrar reserva cancelada")
        void naoEncerraReservaCancelada() {
            reserva.cancelar();

            RegraNegocioException ex = assertThrows(RegraNegocioException.class, reserva::encerrar);
            assertEquals("Somente reservas ativas podem ser encerradas.", ex.getMessage());
            assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
        }
    }
}
