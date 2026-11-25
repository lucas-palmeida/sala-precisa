package br.edu.ifrs.salaprecisa.sistema;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.servico.EncerramentoReservasJob;
import br.edu.ifrs.salaprecisa.sistema.paginas.FormularioReservaPagina;
import br.edu.ifrs.salaprecisa.sistema.paginas.ListaReservasPagina;
import br.edu.ifrs.salaprecisa.sistema.paginas.ListaSalasPagina;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Testes de sistema das reservas (RF04 a RF07), executados em um navegador real.
 */
@DisplayName("Sistema: reserva de salas")
class ReservarSalaSistemaIT extends TesteSistemaBase {

    private static final DateTimeFormatter FORMATO_TELA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private EncerramentoReservasJob encerramentoJob;

    @Test
    @DisplayName("Fluxo principal: a partir da lista de salas, usuário reserva uma sala")
    void reservaSala() {
        salaCadastrada("Lab 101", 30);

        FormularioReservaPagina formulario = ListaSalasPagina.abrir(driver, urlBase()).reservar("Lab 101");
        assertThat(formulario.salaSelecionada()).isEqualTo("Lab 101 (30 pessoas)");

        formulario.solicitante("Ana Souza").periodo(amanha(10, 0), amanha(12, 0));
        evidencia("01-reserva-formulario-preenchido");
        ListaReservasPagina reservas = formulario.reservar();

        evidencia("02-reserva-na-lista");
        assertThat(reservas.mensagemSucesso()).isEqualTo("Reserva registrada com sucesso.");
        assertThat(reservas.sala("Ana Souza")).isEqualTo("Lab 101");
        assertThat(reservas.inicio("Ana Souza")).isEqualTo(FORMATO_TELA.format(amanha(10, 0)));
        assertThat(reservas.fim("Ana Souza")).isEqualTo(FORMATO_TELA.format(amanha(12, 0)));
        assertThat(reservas.status("Ana Souza")).isEqualTo("Ativa");
        assertThat(reservas.podeCancelar("Ana Souza")).isTrue();
    }

    @Test
    @DisplayName("Fluxo alternativo: reserva em horário já ocupado é recusada")
    void conflitoDeHorario() {
        salaCadastrada("Lab 101", 30);
        ListaReservasPagina reservas = ListaReservasPagina.abrir(driver, urlBase())
                .menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Ana").periodo(amanha(10, 0), amanha(12, 0))
                .reservar();
        assertThat(reservas.contemReserva("Ana")).isTrue();

        FormularioReservaPagina formulario = reservas.menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Bruno").periodo(amanha(11, 0), amanha(13, 0))
                .reservarEsperandoErro();

        evidencia("03-reserva-conflito");
        assertThat(formulario.mensagemErro()).isEqualTo("A sala já está reservada em parte do período informado.");
        assertThat(formulario.salaSelecionada()).isEqualTo("Lab 101 (30 pessoas)");
        assertThat(reservaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fluxo alternativo: reserva imediatamente após outra (horário adjacente) é aceita")
    void horarioAdjacente() {
        salaCadastrada("Lab 101", 30);
        ListaReservasPagina.abrir(driver, urlBase()).menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Ana").periodo(amanha(10, 0), amanha(12, 0))
                .reservar();

        ListaReservasPagina reservas = ListaReservasPagina.abrir(driver, urlBase()).menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Bruno").periodo(amanha(12, 0), amanha(13, 0))
                .reservar();

        assertThat(reservas.quantidade()).isEqualTo(2);
        assertThat(reservas.status("Bruno")).isEqualTo("Ativa");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "fim antes do início   | 0  | 12 | 0  | 10 | A data de fim deve ser posterior à data de início.",
        "início no passado     | -2 | 10 | -2 | 11 | Não é possível reservar com início no passado.",
        "duração acima de 8h   | 0  | 8  | 0  | 17 | A reserva não pode ultrapassar 8 horas."
    })
    @DisplayName("Fluxos alternativos: períodos inválidos exibem a mensagem da regra violada")
    void periodosInvalidos(String caso, int diasInicio, int horaInicio, int diasFim, int horaFim, String mensagem) {
        salaCadastrada("Lab 101", 30);
        LocalDateTime inicio = amanha(horaInicio, 0).plusDays(diasInicio);
        LocalDateTime fim = amanha(horaFim, 0).plusDays(diasFim);

        FormularioReservaPagina formulario = ListaReservasPagina.abrir(driver, urlBase()).menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Ana").periodo(inicio, fim)
                .reservarEsperandoErro();

        evidencia("04-periodo-invalido-" + caso.replace(' ', '-'));
        assertThat(formulario.mensagemErro()).isEqualTo(mensagem);
        assertThat(reservaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Fluxo alternativo: campos obrigatórios vazios exibem mensagens por campo")
    void camposObrigatorios() {
        salaCadastrada("Lab 101", 30);

        FormularioReservaPagina formulario = ListaReservasPagina.abrir(driver, urlBase()).menuNovaReserva()
                .solicitante("").periodo(null, null)
                .reservarEsperandoErro();

        evidencia("05-reserva-campos-obrigatorios");
        assertThat(formulario.erroDoCampo("salaId")).isEqualTo("Selecione a sala.");
        assertThat(formulario.erroDoCampo("solicitante")).isEqualTo("Informe o solicitante.");
        assertThat(formulario.erroDoCampo("inicio")).isEqualTo("Informe a data e hora de início.");
        assertThat(formulario.erroDoCampo("fim")).isEqualTo("Informe a data e hora de fim.");
    }

    @Test
    @DisplayName("Fluxo principal: usuário cancela uma reserva e o horário volta a ficar disponível")
    void cancelaReservaELiberaHorario() {
        salaCadastrada("Lab 101", 30);
        ListaReservasPagina reservas = ListaReservasPagina.abrir(driver, urlBase()).menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Ana").periodo(amanha(10, 0), amanha(12, 0))
                .reservar();

        reservas = reservas.cancelar("Ana");

        evidencia("06-reserva-cancelada");
        assertThat(reservas.mensagemSucesso()).isEqualTo("Reserva cancelada.");
        assertThat(reservas.status("Ana")).isEqualTo("Cancelada");
        assertThat(reservas.podeCancelar("Ana")).isFalse();

        reservas = reservas.menuNovaReserva()
                .selecionarSala("Lab 101").solicitante("Bruno").periodo(amanha(10, 0), amanha(12, 0))
                .reservar();

        assertThat(reservas.status("Bruno")).isEqualTo("Ativa");
    }

    @Test
    @DisplayName("Ocupação e liberação: sala aparece Ocupada durante a reserva e é liberada ao fim do período")
    void salaOcupadaELiberadaAutomaticamente() {
        Sala lab = salaCadastrada("Lab 101", 30);
        LocalDateTime agora = LocalDateTime.now();
        // Cenário preparado direto no banco: pela interface não se reserva no passado.
        reservaRepository.save(new Reserva(lab, "Ana", agora.minusHours(1), agora.plusHours(1)));

        ListaSalasPagina salas = ListaSalasPagina.abrir(driver, urlBase());
        evidencia("07-sala-ocupada");
        assertThat(salas.situacao("Lab 101")).isEqualTo("Ocupada");

        // Simula a passagem do tempo: a reserva passa a ter terminado e o job de encerramento é executado.
        reservaRepository.deleteAll();
        reservaRepository.save(new Reserva(lab, "Ana", agora.minusHours(3), agora.minusHours(1)));
        encerramentoJob.executar();

        salas = ListaSalasPagina.abrir(driver, urlBase());
        assertThat(salas.situacao("Lab 101")).isEqualTo("Livre");
        ListaReservasPagina reservas = salas.menuReservas();
        evidencia("08-reserva-encerrada-automaticamente");
        assertThat(reservas.status("Ana")).isEqualTo("Encerrada");
        assertThat(reservas.podeCancelar("Ana")).isFalse();
    }
}
