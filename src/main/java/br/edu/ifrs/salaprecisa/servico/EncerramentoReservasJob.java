package br.edu.ifrs.salaprecisa.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarefa agendada que encerra automaticamente as reservas cujo período terminou,
 * liberando as salas (RF07). O intervalo pode ser ajustado pela propriedade
 * {@code salaprecisa.encerramento.intervalo-ms} (padrão: 60 segundos).
 */
@Component
public class EncerramentoReservasJob {

    private static final Logger LOG = LoggerFactory.getLogger(EncerramentoReservasJob.class);

    private final ReservaService reservaService;

    public EncerramentoReservasJob(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(fixedDelayString = "${salaprecisa.encerramento.intervalo-ms:60000}")
    public int executar() {
        int encerradas = reservaService.encerrarReservasVencidas();
        if (encerradas > 0) {
            LOG.info("{} reserva(s) encerrada(s) automaticamente.", encerradas);
        }
        return encerradas;
    }
}
