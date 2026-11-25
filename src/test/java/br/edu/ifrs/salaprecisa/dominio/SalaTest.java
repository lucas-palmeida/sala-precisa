package br.edu.ifrs.salaprecisa.dominio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes unitários da entidade {@link Sala} e das enumerações do domínio.
 */
@DisplayName("Sala (entidade)")
class SalaTest {

    @Test
    @DisplayName("nova sala guarda os dados informados, começa ativa e sem id")
    void novaSalaComecaAtiva() {
        Sala sala = new Sala("Auditório", TipoSala.AUDITORIO, 200);

        assertAll(
                () -> assertNull(sala.getId()),
                () -> assertEquals("Auditório", sala.getNome()),
                () -> assertEquals(TipoSala.AUDITORIO, sala.getTipo()),
                () -> assertEquals(200, sala.getCapacidade()),
                () -> assertTrue(sala.isAtiva()));
    }

    @Test
    @DisplayName("atualizar altera nome, tipo e capacidade")
    void atualizaDados() {
        Sala sala = new Sala("Sala 1", TipoSala.SALA_REUNIAO, 8);

        sala.atualizar("Sala 1A", TipoSala.LABORATORIO, 20);

        assertAll(
                () -> assertEquals("Sala 1A", sala.getNome()),
                () -> assertEquals(TipoSala.LABORATORIO, sala.getTipo()),
                () -> assertEquals(20, sala.getCapacidade()));
    }

    @Test
    @DisplayName("desativar e ativar alternam a situação da sala")
    void desativaEAtiva() {
        Sala sala = new Sala("Sala 2", TipoSala.OUTRO, 5);

        sala.desativar();
        assertFalse(sala.isAtiva());

        sala.ativar();
        assertTrue(sala.isAtiva());
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({
        "SALA_REUNIAO, Sala de reunião",
        "AUDITORIO, Auditório",
        "LABORATORIO, Laboratório",
        "OUTRO, Outro"
    })
    @DisplayName("TipoSala possui descrição legível")
    void tipoSalaPossuiDescricao(TipoSala tipo, String descricao) {
        assertEquals(descricao, tipo.getDescricao());
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource({
        "ATIVA, Ativa",
        "CANCELADA, Cancelada",
        "ENCERRADA, Encerrada"
    })
    @DisplayName("StatusReserva possui descrição legível")
    void statusReservaPossuiDescricao(StatusReserva status, String descricao) {
        assertEquals(descricao, status.getDescricao());
    }
}
