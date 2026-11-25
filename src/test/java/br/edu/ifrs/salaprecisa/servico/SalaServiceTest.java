package br.edu.ifrs.salaprecisa.servico;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
 * Testes unitários de {@link SalaService}.
 *
 * <p>Os repositórios são substituídos por mocks do Mockito, isolando a regra de negócio
 * do banco de dados. O relógio é um {@link Clock#fixed relógio fixo}, o que torna
 * determinístico o cálculo de "salas ocupadas agora".</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalaService")
class SalaServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2027, 1, 10, 8, 0);

    @Mock
    private SalaRepository salaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Captor
    private ArgumentCaptor<Sala> salaCaptor;

    private SalaService service;

    @BeforeEach
    void criarServico() {
        Clock relogio = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        service = new SalaService(salaRepository, reservaRepository, relogio);
    }

    /** Cria uma sala com id preenchido, simulando uma sala já persistida. */
    private static Sala salaPersistida(Long id, String nome) {
        Sala sala = new Sala(nome, TipoSala.SALA_REUNIAO, 10);
        ReflectionTestUtils.setField(sala, "id", id);
        return sala;
    }

    @Nested
    @DisplayName("cadastrar")
    class Cadastrar {

        @Test
        @DisplayName("salva a sala com o nome sem espaços nas extremidades")
        void cadastraComSucesso() {
            when(salaRepository.existsByNomeIgnoreCase("Lab 101")).thenReturn(false);
            when(salaRepository.save(any(Sala.class))).thenAnswer(inv -> inv.getArgument(0));

            Sala resultado = service.cadastrar("  Lab 101  ", TipoSala.LABORATORIO, 30);

            verify(salaRepository).save(salaCaptor.capture());
            Sala salva = salaCaptor.getValue();
            assertAll(
                    () -> assertSame(salva, resultado),
                    () -> assertEquals("Lab 101", salva.getNome()),
                    () -> assertEquals(TipoSala.LABORATORIO, salva.getTipo()),
                    () -> assertEquals(30, salva.getCapacidade()),
                    () -> assertTrue(salva.isAtiva()));
        }

        @Test
        @DisplayName("rejeita nome já existente (sem diferenciar maiúsculas) e não salva")
        void rejeitaNomeDuplicado() {
            when(salaRepository.existsByNomeIgnoreCase("lab 101")).thenReturn(true);

            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar("lab 101", TipoSala.AUDITORIO, 50));

            assertEquals("Já existe uma sala com o nome informado.", ex.getMessage());
            verify(salaRepository, never()).save(any());
        }

        @ParameterizedTest(name = "nome inválido: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("rejeita nome nulo ou em branco")
        void rejeitaNomeEmBranco(String nome) {
            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar(nome, TipoSala.OUTRO, 10));

            assertEquals("O nome da sala é obrigatório.", ex.getMessage());
            verify(salaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita tipo nulo")
        void rejeitaTipoNulo() {
            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar("Sala", null, 10));

            assertEquals("O tipo da sala é obrigatório.", ex.getMessage());
        }

        @ParameterizedTest(name = "capacidade inválida: {0}")
        @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1001})
        @DisplayName("rejeita capacidade fora do intervalo [1, 1000]")
        void rejeitaCapacidadeInvalida(int capacidade) {
            RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                    () -> service.cadastrar("Sala", TipoSala.OUTRO, capacidade));

            assertEquals("A capacidade deve estar entre 1 e 1000.", ex.getMessage());
            verify(salaRepository, never()).save(any());
        }

        @ParameterizedTest(name = "capacidade limite válida: {0}")
        @ValueSource(ints = {1, 1000})
        @DisplayName("aceita capacidades nos limites do intervalo")
        void aceitaCapacidadeNosLimites(int capacidade) {
            when(salaRepository.save(any(Sala.class))).thenAnswer(inv -> inv.getArgument(0));

            Sala sala = service.cadastrar("Sala", TipoSala.OUTRO, capacidade);

            assertEquals(capacidade, sala.getCapacidade());
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("altera os dados de uma sala existente")
        void atualizaComSucesso() {
            Sala existente = salaPersistida(1L, "Sala 1");
            when(salaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(salaRepository.existsByNomeIgnoreCaseAndIdNot("Sala 1A", 1L)).thenReturn(false);
            when(salaRepository.save(existente)).thenReturn(existente);

            Sala resultado = service.atualizar(1L, " Sala 1A ", TipoSala.AUDITORIO, 99);

            assertAll(
                    () -> assertEquals("Sala 1A", resultado.getNome()),
                    () -> assertEquals(TipoSala.AUDITORIO, resultado.getTipo()),
                    () -> assertEquals(99, resultado.getCapacidade()));
        }

        @Test
        @DisplayName("rejeita nome que pertence a outra sala")
        void rejeitaNomeDeOutraSala() {
            Sala existente = salaPersistida(1L, "Sala 1");
            when(salaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(salaRepository.existsByNomeIgnoreCaseAndIdNot("Sala 2", 1L)).thenReturn(true);

            assertThrows(RegraNegocioException.class,
                    () -> service.atualizar(1L, "Sala 2", TipoSala.OUTRO, 10));

            assertEquals("Sala 1", existente.getNome());
            verify(salaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita dados inválidos sem alterar a sala")
        void rejeitaDadosInvalidos() {
            Sala existente = salaPersistida(1L, "Sala 1");
            when(salaRepository.findById(1L)).thenReturn(Optional.of(existente));

            assertThrows(RegraNegocioException.class,
                    () -> service.atualizar(1L, "Sala 1", TipoSala.OUTRO, 0));

            assertEquals(10, existente.getCapacidade());
            verify(salaRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança exceção quando a sala não existe")
        void salaInexistente() {
            when(salaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RecursoNaoEncontradoException.class,
                    () -> service.atualizar(99L, "X", TipoSala.OUTRO, 10));
        }
    }

    @Nested
    @DisplayName("desativar / ativar")
    class DesativarAtivar {

        @Test
        @DisplayName("desativar marca a sala como inativa e salva")
        void desativa() {
            Sala sala = salaPersistida(1L, "Sala 1");
            when(salaRepository.findById(1L)).thenReturn(Optional.of(sala));
            when(salaRepository.save(sala)).thenReturn(sala);

            Sala resultado = service.desativar(1L);

            assertFalse(resultado.isAtiva());
            verify(salaRepository).save(sala);
        }

        @Test
        @DisplayName("ativar marca a sala como ativa e salva")
        void ativa() {
            Sala sala = salaPersistida(1L, "Sala 1");
            sala.desativar();
            when(salaRepository.findById(1L)).thenReturn(Optional.of(sala));
            when(salaRepository.save(sala)).thenReturn(sala);

            Sala resultado = service.ativar(1L);

            assertTrue(resultado.isAtiva());
            verify(salaRepository).save(sala);
        }

        @Test
        @DisplayName("desativar sala inexistente lança exceção")
        void desativaInexistente() {
            when(salaRepository.findById(5L)).thenReturn(Optional.empty());

            RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                    () -> service.desativar(5L));
            assertEquals("Sala não encontrada: 5", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("buscarPorId devolve a sala encontrada")
        void buscaPorId() {
            Sala sala = salaPersistida(3L, "Sala 3");
            when(salaRepository.findById(3L)).thenReturn(Optional.of(sala));

            assertSame(sala, service.buscarPorId(3L));
        }

        @Test
        @DisplayName("listar e listarAtivas delegam ao repositório com ordenação por nome")
        void listagens() {
            List<Sala> todas = List.of(salaPersistida(1L, "A"), salaPersistida(2L, "B"));
            List<Sala> ativas = List.of(salaPersistida(1L, "A"));
            when(salaRepository.findAllByOrderByNomeAsc()).thenReturn(todas);
            when(salaRepository.findByAtivaTrueOrderByNomeAsc()).thenReturn(ativas);

            assertEquals(todas, service.listar());
            assertEquals(ativas, service.listarAtivas());
        }

        @Test
        @DisplayName("idsSalasOcupadasAgora consulta reservas ATIVAS no instante do relógio")
        void consultaSalasOcupadasNoInstanteAtual() {
            when(reservaRepository.idsSalasOcupadas(AGORA, StatusReserva.ATIVA)).thenReturn(List.of(1L, 4L));

            Set<Long> ocupadas = service.idsSalasOcupadasAgora();

            assertEquals(Set.of(1L, 4L), ocupadas);
            verify(reservaRepository).idsSalasOcupadas(AGORA, StatusReserva.ATIVA);
        }
    }
}
