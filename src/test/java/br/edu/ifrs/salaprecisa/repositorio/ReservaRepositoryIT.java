package br.edu.ifrs.salaprecisa.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * Teste de componente da camada de persistência de reservas.
 *
 * <p>O {@link DataJpaTest} sobe apenas a infraestrutura JPA (entidades, repositórios e
 * banco H2 em memória), sem a camada web e sem os serviços. Assim as consultas JPQL de
 * {@link ReservaRepository} são executadas contra um banco de dados real. Cada teste roda
 * em uma transação desfeita ao final, garantindo isolamento entre os cenários.</p>
 */
@DataJpaTest
@DisplayName("ReservaRepository (componente de persistência)")
class ReservaRepositoryIT {

    private static final LocalDateTime DIA = LocalDateTime.of(2027, 3, 15, 0, 0);

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private SalaRepository salaRepository;

    private Sala lab;
    private Sala auditorio;

    @BeforeEach
    void criarSalas() {
        lab = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
        auditorio = salaRepository.save(new Sala("Auditório", TipoSala.AUDITORIO, 200));
    }

    private static LocalDateTime hora(String hhmm) {
        return LocalDateTime.parse(DIA.toLocalDate() + "T" + hhmm);
    }

    private Reserva reservar(Sala sala, String inicio, String fim) {
        return reservaRepository.saveAndFlush(new Reserva(sala, "Ana", hora(inicio), hora(fim)));
    }

    @Nested
    @DisplayName("existeReservaSobreposta")
    class ExisteReservaSobreposta {

        /*
         * Mesmos casos de valor-limite usados no teste unitário da entidade Reserva,
         * agora executados pela consulta JPQL no banco: garante que a regra de sobreposição
         * do banco é idêntica à do domínio (intervalo semiaberto [inicio, fim)).
         */
        @ParameterizedTest(name = "[{0} – {1}) conflita com [10:00 – 12:00)? {2}")
        @CsvSource({
            "07:00, 08:00, false",
            "09:00, 10:00, false",
            "09:00, 10:01, true",
            "10:00, 12:00, true",
            "10:30, 11:30, true",
            "09:00, 13:00, true",
            "11:59, 13:00, true",
            "12:00, 13:00, false",
            "13:00, 14:00, false"
        })
        void detectaSobreposicaoNoBanco(String inicio, String fim, boolean esperado) {
            reservar(lab, "10:00", "12:00");

            boolean conflito = reservaRepository.existeReservaSobreposta(
                    lab.getId(), hora(inicio), hora(fim), StatusReserva.ATIVA);

            assertThat(conflito).isEqualTo(esperado);
        }

        @Test
        @DisplayName("reservas de outra sala não geram conflito")
        void outraSalaNaoConflita() {
            reservar(auditorio, "10:00", "12:00");

            assertThat(reservaRepository.existeReservaSobreposta(
                    lab.getId(), hora("10:00"), hora("12:00"), StatusReserva.ATIVA)).isFalse();
        }

        @Test
        @DisplayName("reservas canceladas ou encerradas não geram conflito")
        void reservasInativasNaoConflitam() {
            Reserva cancelada = reservar(lab, "10:00", "12:00");
            cancelada.cancelar();
            Reserva encerrada = reservar(lab, "13:00", "15:00");
            encerrada.encerrar();
            reservaRepository.saveAllAndFlush(List.of(cancelada, encerrada));

            assertThat(reservaRepository.existeReservaSobreposta(
                    lab.getId(), hora("09:00"), hora("16:00"), StatusReserva.ATIVA)).isFalse();
        }
    }

    @Nested
    @DisplayName("idsSalasOcupadas")
    class IdsSalasOcupadas {

        @Test
        @DisplayName("devolve as salas com reserva ativa em andamento no instante")
        void salasOcupadasNoInstante() {
            reservar(lab, "10:00", "12:00");
            reservar(auditorio, "14:00", "16:00");

            assertThat(reservaRepository.idsSalasOcupadas(hora("11:00"), StatusReserva.ATIVA))
                    .containsExactly(lab.getId());
        }

        @Test
        @DisplayName("considera o início inclusivo e o fim exclusivo")
        void limitesDoPeriodo() {
            reservar(lab, "10:00", "12:00");

            assertThat(reservaRepository.idsSalasOcupadas(hora("10:00"), StatusReserva.ATIVA))
                    .containsExactly(lab.getId());
            assertThat(reservaRepository.idsSalasOcupadas(hora("12:00"), StatusReserva.ATIVA))
                    .isEmpty();
        }

        @Test
        @DisplayName("não repete a sala e ignora reservas canceladas")
        void semRepeticaoEIgnoraCanceladas() {
            reservar(lab, "10:00", "12:00");
            reservar(lab, "11:00", "13:00"); // situação anômala: inserida direto no banco
            Reserva cancelada = reservar(auditorio, "10:00", "12:00");
            cancelada.cancelar();
            reservaRepository.saveAndFlush(cancelada);

            assertThat(reservaRepository.idsSalasOcupadas(hora("11:30"), StatusReserva.ATIVA))
                    .containsExactly(lab.getId());
        }
    }

    @Nested
    @DisplayName("consultas derivadas")
    class ConsultasDerivadas {

        @Test
        @DisplayName("findByStatusAndFimLessThanEqual inclui reserva que termina exatamente no instante")
        void reservasVencidas() {
            Reserva terminouAntes = reservar(lab, "08:00", "09:00");
            Reserva terminaAgora = reservar(lab, "09:00", "10:00");
            reservar(lab, "10:00", "11:00");

            assertThat(reservaRepository.findByStatusAndFimLessThanEqual(StatusReserva.ATIVA, hora("10:00")))
                    .containsExactlyInAnyOrder(terminouAntes, terminaAgora);
        }

        @Test
        @DisplayName("findAllByOrderByInicioDesc ordena da mais recente para a mais antiga")
        void listaOrdenadaPorInicioDecrescente() {
            Reserva primeira = reservar(lab, "08:00", "09:00");
            Reserva terceira = reservar(auditorio, "14:00", "15:00");
            Reserva segunda = reservar(lab, "10:00", "11:00");

            assertThat(reservaRepository.findAllByOrderByInicioDesc())
                    .containsExactly(terceira, segunda, primeira);
        }

        @Test
        @DisplayName("findBySalaIdOrderByInicioAsc filtra pela sala e ordena por início")
        void listaPorSala() {
            Reserva tarde = reservar(lab, "14:00", "15:00");
            reservar(auditorio, "09:00", "10:00");
            Reserva manha = reservar(lab, "08:00", "09:00");

            assertThat(reservaRepository.findBySalaIdOrderByInicioAsc(lab.getId()))
                    .containsExactly(manha, tarde);
        }
    }
}
