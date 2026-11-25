package br.edu.ifrs.salaprecisa.servico;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.excecao.ConflitoReservaException;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Testes unitários de {@link ReservaService}, classe que concentra as principais regras
 * de negócio do sistema (RF04 a RF07).
 *
 * <p>Os repositórios são mocks do Mockito. O relógio do sistema é fixado em
 * <b>10/01/2027 08:00</b>, o que permite testar com precisão as regras que dependem
 * da data/hora atual (início no passado e encerramento automático).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService")
class ReservaServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2027, 1, 10, 8, 0);
    private static final Long SALA_ID = 1L;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SalaRepository salaRepository;

    @Captor
    private ArgumentCaptor<Reserva> reservaCaptor;

    private ReservaService service;
    private Sala sala;

    @BeforeEach
    void criarServico() {
        Clock relogio = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        service = new ReservaService(reservaRepository, salaRepository, relogio);
        sala = new Sala("Lab 101", TipoSala.LABORATORIO, 30);
        ReflectionTestUtils.setField(sala, "id", SALA_ID);
    }

    /** Configura o cenário em que a sala existe, está ativa e não há conflito de horário. */
    private void salaDisponivel() {
        when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));
        when(reservaRepository.existeReservaSobreposta(any(), any(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("cria reserva ATIVA quando a sala está disponível")
        void criaComSucesso() {
            salaDisponivel();
            LocalDateTime inicio = AGORA.plusHours(2);
            LocalDateTime fim = AGORA.plusHours(4);

            Reserva resultado = service.criar(SALA_ID, "  Ana Souza ", inicio, fim);

            verify(reservaRepository).existeReservaSobreposta(SALA_ID, inicio, fim, StatusReserva.ATIVA);
            verify(reservaRepository).save(reservaCaptor.capture());
            Reserva salva = reservaCaptor.getValue();
            assertAll(
                    () -> assertSame(salva, resultado),
                    () -> assertSame(sala, salva.getSala()),
                    () -> assertEquals("Ana Souza", salva.getSolicitante()),
                    () -> assertEquals(inicio, salva.getInicio()),
                    () -> assertEquals(fim, salva.getFim()),
                    () -> assertEquals(StatusReserva.ATIVA, salva.getStatus()));
        }

        @Test
        @DisplayName("lança ConflitoReservaException quando há reserva sobreposta e não salva")
        void rejeitaConflito() {
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));
            when(reservaRepository.existeReservaSobreposta(any(), any(), any(), any())).thenReturn(true);

            ConflitoReservaException ex = assertThrows(ConflitoReservaException.class,
                    () -> service.criar(SALA_ID, "Bia", AGORA.plusHours(1), AGORA.plusHours(2)));

            assertEquals("A sala já está reservada em parte do período informado.", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando a sala não existe")
        void rejeitaSalaInexistente() {
            when(salaRepository.findById(99L)).thenReturn(Optional.empty());

            RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                    () -> service.criar(99L, "Ana", AGORA.plusHours(1), AGORA.plusHours(2)));

            assertEquals("Sala não encontrada: 99", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita reserva em sala inativa")
        void rejeitaSalaInativa() {
            sala.desativar();
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));

            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.criar(SALA_ID, "Ana", AGORA.plusHours(1), AGORA.plusHours(2)));

            assertEquals("A sala está inativa e não aceita reservas.", ex.getMessage());
            verify(reservaRepository, never()).existeReservaSobreposta(any(), any(), any(), any());
        }

        @ParameterizedTest(name = "solicitante inválido: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("rejeita solicitante nulo ou em branco")
        void rejeitaSolicitanteEmBranco(String solicitante) {
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));

            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.criar(SALA_ID, solicitante, AGORA.plusHours(1), AGORA.plusHours(2)));

            assertEquals("O solicitante é obrigatório.", ex.getMessage());
        }

        @Test
        @DisplayName("rejeita datas nulas")
        void rejeitaDatasNulas() {
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));

            assertAll(
                    () -> assertThrows(RegraNegocioException.class,
                            () -> service.criar(SALA_ID, "Ana", null, AGORA.plusHours(2))),
                    () -> assertThrows(RegraNegocioException.class,
                            () -> service.criar(SALA_ID, "Ana", AGORA.plusHours(1), null)));
            verify(reservaRepository, never()).save(any());
        }

        @ParameterizedTest(name = "fim {0} minuto(s) após o início")
        @ValueSource(ints = {0, -1, -60})
        @DisplayName("rejeita fim igual ou anterior ao início")
        void rejeitaFimNaoPosterior(int minutosAposInicio) {
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));
            LocalDateTime inicio = AGORA.plusHours(1);

            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.criar(SALA_ID, "Ana", inicio, inicio.plusMinutes(minutosAposInicio)));

            assertEquals("A data de fim deve ser posterior à data de início.", ex.getMessage());
        }

        @Test
        @DisplayName("rejeita início no passado (1 minuto antes de agora)")
        void rejeitaInicioNoPassado() {
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));

            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.criar(SALA_ID, "Ana", AGORA.minusMinutes(1), AGORA.plusHours(1)));

            assertEquals("Não é possível reservar com início no passado.", ex.getMessage());
        }

        @Test
        @DisplayName("aceita início exatamente no instante atual (valor-limite)")
        void aceitaInicioAgora() {
            salaDisponivel();

            Reserva reserva = service.criar(SALA_ID, "Ana", AGORA, AGORA.plusHours(1));

            assertEquals(AGORA, reserva.getInicio());
        }

        @ParameterizedTest(name = "duração de {0} minuto(s) é aceita")
        @ValueSource(ints = {1, 60, 479, 480})
        @DisplayName("aceita durações de 1 minuto até exatamente 8 horas")
        void aceitaDuracaoValida(int minutos) {
            salaDisponivel();
            LocalDateTime inicio = AGORA.plusDays(1);

            Reserva reserva = service.criar(SALA_ID, "Ana", inicio, inicio.plusMinutes(minutos));

            assertEquals(inicio.plusMinutes(minutos), reserva.getFim());
        }

        @ParameterizedTest(name = "duração de {0} minutos é rejeitada")
        @ValueSource(ints = {481, 600, 1440})
        @DisplayName("rejeita durações acima de 8 horas")
        void rejeitaDuracaoExcessiva(int minutos) {
            when(salaRepository.findById(SALA_ID)).thenReturn(Optional.of(sala));
            LocalDateTime inicio = AGORA.plusDays(1);

            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.criar(SALA_ID, "Ana", inicio, inicio.plusMinutes(minutos)));

            assertEquals("A reserva não pode ultrapassar 8 horas.", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelar")
    class Cancelar {

        @Test
        @DisplayName("cancela reserva ativa e salva a alteração")
        void cancelaReservaAtiva() {
            Reserva reserva = new Reserva(sala, "Ana", AGORA.plusHours(1), AGORA.plusHours(2));
            when(reservaRepository.findById(7L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(reserva)).thenReturn(reserva);

            Reserva resultado = service.cancelar(7L);

            assertEquals(StatusReserva.CANCELADA, resultado.getStatus());
            verify(reservaRepository).save(reserva);
        }

        @Test
        @DisplayName("não permite cancelar reserva já cancelada e não salva")
        void naoCancelaDuasVezes() {
            Reserva reserva = new Reserva(sala, "Ana", AGORA.plusHours(1), AGORA.plusHours(2));
            reserva.cancelar();
            when(reservaRepository.findById(7L)).thenReturn(Optional.of(reserva));

            assertThrows(RegraNegocioException.class, () -> service.cancelar(7L));
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança exceção quando a reserva não existe")
        void reservaInexistente() {
            when(reservaRepository.findById(42L)).thenReturn(Optional.empty());

            RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                    () -> service.cancelar(42L));
            assertEquals("Reserva não encontrada: 42", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("encerrarReservasVencidas")
    class EncerrarVencidas {

        @Test
        @DisplayName("encerra todas as reservas ativas cujo fim já passou")
        void encerraVencidas() {
            Reserva r1 = new Reserva(sala, "Ana", AGORA.minusHours(3), AGORA.minusHours(1));
            Reserva r2 = new Reserva(sala, "Bia", AGORA.minusHours(1), AGORA);
            List<Reserva> vencidas = List.of(r1, r2);
            when(reservaRepository.findByStatusAndFimLessThanEqual(StatusReserva.ATIVA, AGORA)).thenReturn(vencidas);

            int quantidade = service.encerrarReservasVencidas();

            assertEquals(2, quantidade);
            assertEquals(StatusReserva.ENCERRADA, r1.getStatus());
            assertEquals(StatusReserva.ENCERRADA, r2.getStatus());
            verify(reservaRepository).saveAll(vencidas);
        }

        @Test
        @DisplayName("devolve zero quando não há reservas vencidas")
        void nenhumaVencida() {
            when(reservaRepository.findByStatusAndFimLessThanEqual(StatusReserva.ATIVA, AGORA))
                    .thenReturn(List.of());

            assertEquals(0, service.encerrarReservasVencidas());
            verify(reservaRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("buscarPorId devolve a reserva encontrada")
        void buscaPorId() {
            Reserva reserva = new Reserva(sala, "Ana", AGORA.plusHours(1), AGORA.plusHours(2));
            when(reservaRepository.findById(3L)).thenReturn(Optional.of(reserva));

            assertSame(reserva, service.buscarPorId(3L));
        }

        @Test
        @DisplayName("listar devolve as reservas ordenadas pelo repositório")
        void lista() {
            List<Reserva> reservas = List.of(new Reserva(sala, "Ana", AGORA, AGORA.plusHours(1)));
            when(reservaRepository.findAllByOrderByInicioDesc()).thenReturn(reservas);

            assertEquals(reservas, service.listar());
        }

        @Test
        @DisplayName("listarPorSala devolve as reservas de uma sala existente")
        void listaPorSala() {
            List<Reserva> reservas = List.of(new Reserva(sala, "Ana", AGORA, AGORA.plusHours(1)));
            when(salaRepository.existsById(SALA_ID)).thenReturn(true);
            when(reservaRepository.findBySalaIdOrderByInicioAsc(SALA_ID)).thenReturn(reservas);

            assertEquals(reservas, service.listarPorSala(SALA_ID));
        }

        @Test
        @DisplayName("listarPorSala lança exceção para sala inexistente")
        void listaPorSalaInexistente() {
            when(salaRepository.existsById(99L)).thenReturn(false);

            assertThrows(RecursoNaoEncontradoException.class, () -> service.listarPorSala(99L));
            verify(reservaRepository, never()).findBySalaIdOrderByInicioAsc(any());
        }
    }
}
