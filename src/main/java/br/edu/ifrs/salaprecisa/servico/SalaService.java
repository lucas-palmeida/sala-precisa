package br.edu.ifrs.salaprecisa.servico;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio relacionadas ao cadastro de salas.
 */
@Service
public class SalaService {

    /** Capacidade máxima aceita para uma sala. */
    public static final int CAPACIDADE_MAXIMA = 1000;

    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public SalaService(SalaRepository salaRepository, ReservaRepository reservaRepository, Clock clock) {
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
    }

    @Transactional
    public Sala cadastrar(String nome, TipoSala tipo, int capacidade) {
        String nomeNormalizado = validarDados(nome, tipo, capacidade);
        if (salaRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new RegraNegocioException("Já existe uma sala com o nome informado.");
        }
        return salaRepository.save(new Sala(nomeNormalizado, tipo, capacidade));
    }

    @Transactional
    public Sala atualizar(Long id, String nome, TipoSala tipo, int capacidade) {
        Sala sala = buscarPorId(id);
        String nomeNormalizado = validarDados(nome, tipo, capacidade);
        if (salaRepository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, id)) {
            throw new RegraNegocioException("Já existe uma sala com o nome informado.");
        }
        sala.atualizar(nomeNormalizado, tipo, capacidade);
        return salaRepository.save(sala);
    }

    @Transactional
    public Sala desativar(Long id) {
        Sala sala = buscarPorId(id);
        sala.desativar();
        return salaRepository.save(sala);
    }

    @Transactional
    public Sala ativar(Long id) {
        Sala sala = buscarPorId(id);
        sala.ativar();
        return salaRepository.save(sala);
    }

    @Transactional(readOnly = true)
    public Sala buscarPorId(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Sala> listar() {
        return salaRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Sala> listarAtivas() {
        return salaRepository.findByAtivaTrueOrderByNomeAsc();
    }

    /** Identificadores das salas ocupadas (com reserva ativa em andamento) neste momento. */
    @Transactional(readOnly = true)
    public Set<Long> idsSalasOcupadasAgora() {
        LocalDateTime agora = LocalDateTime.now(clock);
        return new HashSet<>(reservaRepository.idsSalasOcupadas(agora, StatusReserva.ATIVA));
    }

    private String validarDados(String nome, TipoSala tipo, int capacidade) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da sala é obrigatório.");
        }
        if (tipo == null) {
            throw new RegraNegocioException("O tipo da sala é obrigatório.");
        }
        if (capacidade < 1 || capacidade > CAPACIDADE_MAXIMA) {
            throw new RegraNegocioException(
                    "A capacidade deve estar entre 1 e " + CAPACIDADE_MAXIMA + ".");
        }
        return nome.trim();
    }
}
