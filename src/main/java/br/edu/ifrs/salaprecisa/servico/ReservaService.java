package br.edu.ifrs.salaprecisa.servico;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import br.edu.ifrs.salaprecisa.excecao.ConflitoReservaException;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio relacionadas às reservas de salas.
 */
@Service
public class ReservaService {

    /** Duração máxima permitida para uma reserva. */
    public static final Duration DURACAO_MAXIMA = Duration.ofHours(8);

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final Clock clock;

    public ReservaService(ReservaRepository reservaRepository, SalaRepository salaRepository, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.clock = clock;
    }

    /**
     * Cria uma reserva, desde que a sala exista e esteja ativa, o período seja válido
     * e não haja outra reserva ativa sobreposta para a mesma sala.
     */
    @Transactional
    public Reserva criar(Long salaId, String solicitante, LocalDateTime inicio, LocalDateTime fim) {
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada: " + salaId));
        if (!sala.isAtiva()) {
            throw new RegraNegocioException("A sala está inativa e não aceita reservas.");
        }
        if (solicitante == null || solicitante.isBlank()) {
            throw new RegraNegocioException("O solicitante é obrigatório.");
        }
        validarPeriodo(inicio, fim);
        validarAntecedenciaEDuracao(inicio, fim);
        if (reservaRepository.existeReservaSobreposta(salaId, inicio, fim, StatusReserva.ATIVA)) {
            throw new ConflitoReservaException("A sala já está reservada em parte do período informado.");
        }
        return reservaRepository.save(new Reserva(sala, solicitante.trim(), inicio, fim));
    }

    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.cancelar();
        return reservaRepository.save(reserva);
    }

    /**
     * Encerra todas as reservas ativas cujo período já terminou, liberando as salas.
     *
     * @return quantidade de reservas encerradas
     */
    @Transactional
    public int encerrarReservasVencidas() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Reserva> vencidas = reservaRepository.findByStatusAndFimLessThanEqual(StatusReserva.ATIVA, agora);
        for (Reserva reserva : vencidas) {
            reserva.encerrar();
        }
        reservaRepository.saveAll(vencidas);
        return vencidas.size();
    }

    @Transactional(readOnly = true)
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listar() {
        return reservaRepository.findAllByOrderByInicioDesc();
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorSala(Long salaId) {
        if (!salaRepository.existsById(salaId)) {
            throw new RecursoNaoEncontradoException("Sala não encontrada: " + salaId);
        }
        return reservaRepository.findBySalaIdOrderByInicioAsc(salaId);
    }

    /** Verifica se as datas foram informadas e se o fim é posterior ao início. */
    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("As datas de início e fim são obrigatórias.");
        }
        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException("A data de fim deve ser posterior à data de início.");
        }
    }

    /** Verifica se o início não está no passado e se a duração não excede o máximo permitido. */
    private void validarAntecedenciaEDuracao(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new RegraNegocioException("Não é possível reservar com início no passado.");
        }
        if (Duration.between(inicio, fim).compareTo(DURACAO_MAXIMA) > 0) {
            throw new RegraNegocioException("A reserva não pode ultrapassar "
                    + DURACAO_MAXIMA.toHours() + " horas.");
        }
    }
}
