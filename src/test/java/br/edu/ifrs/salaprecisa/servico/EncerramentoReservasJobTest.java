package br.edu.ifrs.salaprecisa.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários de {@link EncerramentoReservasJob}.
 *
 * <p>O {@link ReservaService} é substituído por um mock e injetado automaticamente
 * no job via {@link InjectMocks}. Assim verifica-se apenas a responsabilidade do job:
 * acionar o serviço e repassar a quantidade de reservas encerradas.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EncerramentoReservasJob")
class EncerramentoReservasJobTest {

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private EncerramentoReservasJob job;

    @Test
    @DisplayName("aciona o serviço uma vez e devolve a quantidade encerrada")
    void encerraReservasVencidas() {
        when(reservaService.encerrarReservasVencidas()).thenReturn(3);

        int encerradas = job.executar();

        assertEquals(3, encerradas);
        verify(reservaService, times(1)).encerrarReservasVencidas();
    }

    @Test
    @DisplayName("devolve zero quando não há reservas a encerrar")
    void nadaAEncerrar() {
        when(reservaService.encerrarReservasVencidas()).thenReturn(0);

        assertEquals(0, job.executar());
        verify(reservaService).encerrarReservasVencidas();
    }
}
