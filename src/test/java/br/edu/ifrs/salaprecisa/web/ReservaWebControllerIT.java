package br.edu.ifrs.salaprecisa.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.excecao.ConflitoReservaException;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.servico.ReservaService;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de componente da camada web de reservas (controller + templates Thymeleaf),
 * com os serviços substituídos por mocks.
 */
@WebMvcTest(ReservaWebController.class)
@DisplayName("Telas de reservas (componente web)")
class ReservaWebControllerIT {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 20, 14, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2030, 5, 20, 16, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private SalaService salaService;

    private Sala lab;

    @BeforeEach
    void criarSala() {
        lab = new Sala("Lab 101", TipoSala.LABORATORIO, 30);
        ReflectionTestUtils.setField(lab, "id", 1L);
        when(salaService.listarAtivas()).thenReturn(List.of(lab));
    }

    @Nested
    @DisplayName("GET /reservas")
    class Listar {

        @Test
        @DisplayName("exibe as reservas com datas formatadas e botão de cancelar apenas nas ativas")
        void exibeReservas() throws Exception {
            Reserva ativa = new Reserva(lab, "Ana Souza", INICIO, FIM);
            ReflectionTestUtils.setField(ativa, "id", 10L);
            Reserva cancelada = new Reserva(lab, "Bruno", INICIO.plusDays(1), FIM.plusDays(1));
            ReflectionTestUtils.setField(cancelada, "id", 11L);
            cancelada.cancelar();
            when(reservaService.listar()).thenReturn(List.of(ativa, cancelada));

            mvc.perform(get("/reservas"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reservas/lista"))
                    .andExpect(content().string(containsString("Ana Souza")))
                    .andExpect(content().string(containsString("20/05/2030 14:00")))
                    .andExpect(content().string(containsString("Cancelada")))
                    .andExpect(content().string(containsString("/reservas/10/cancelar")))
                    .andExpect(content().string(not(containsString("/reservas/11/cancelar"))));
        }

        @Test
        @DisplayName("exibe mensagem quando não há reservas")
        void listaVazia() throws Exception {
            when(reservaService.listar()).thenReturn(List.of());

            mvc.perform(get("/reservas"))
                    .andExpect(content().string(containsString("Nenhuma reserva registrada.")));
        }
    }

    @Nested
    @DisplayName("GET /reservas/nova")
    class Formulario {

        @Test
        @DisplayName("pré-seleciona a sala informada na URL e lista apenas salas ativas")
        void preSelecionaSala() throws Exception {
            mvc.perform(get("/reservas/nova").param("salaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reservas/form"))
                    .andExpect(model().attribute("reservaForm", hasProperty("salaId", is(1L))))
                    .andExpect(model().attribute("salas", List.of(lab)))
                    .andExpect(content().string(containsString("Lab 101 (30 pessoas)")));
        }

        @Test
        @DisplayName("informa quando não há salas ativas")
        void semSalasAtivas() throws Exception {
            when(salaService.listarAtivas()).thenReturn(List.of());

            mvc.perform(get("/reservas/nova"))
                    .andExpect(content().string(containsString("Não há salas ativas.")));
        }
    }

    @Nested
    @DisplayName("POST /reservas")
    class Criar {

        @Test
        @DisplayName("dados válidos: converte as datas do campo datetime-local, cria e redireciona")
        void criaComSucesso() throws Exception {
            mvc.perform(post("/reservas")
                            .param("salaId", "1")
                            .param("solicitante", "Ana")
                            .param("inicio", "2030-05-20T14:00")
                            .param("fim", "2030-05-20T16:00"))
                    .andExpect(redirectedUrl("/reservas"))
                    .andExpect(flash().attribute("sucesso", "Reserva registrada com sucesso."));

            verify(reservaService).criar(1L, "Ana", INICIO, FIM);
        }

        @Test
        @DisplayName("campos vazios: reexibe o formulário com erros e não chama o serviço")
        void camposVazios() throws Exception {
            mvc.perform(post("/reservas")
                            .param("salaId", "").param("solicitante", "").param("inicio", "").param("fim", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reservas/form"))
                    .andExpect(model().attributeHasFieldErrors("reservaForm", "salaId", "solicitante", "inicio", "fim"))
                    .andExpect(model().attributeExists("salas"));

            verify(reservaService, never()).criar(anyLong(), anyString(), any(), any());
        }

        @Test
        @DisplayName("data em formato inválido é tratada como erro de campo")
        void dataInvalida() throws Exception {
            mvc.perform(post("/reservas")
                            .param("salaId", "1").param("solicitante", "Ana")
                            .param("inicio", "amanhã").param("fim", "2030-05-20T16:00"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("reservaForm", "inicio"));
        }

        @Test
        @DisplayName("conflito de horário: reexibe o formulário com a mensagem")
        void conflito() throws Exception {
            when(reservaService.criar(1L, "Ana", INICIO, FIM))
                    .thenThrow(new ConflitoReservaException("A sala já está reservada em parte do período informado."));

            mvc.perform(post("/reservas")
                            .param("salaId", "1").param("solicitante", "Ana")
                            .param("inicio", "2030-05-20T14:00").param("fim", "2030-05-20T16:00"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reservas/form"))
                    .andExpect(content().string(containsString("A sala já está reservada em parte do período")))
                    .andExpect(content().string(containsString("value=\"2030-05-20T14:00\"")));
        }

        @Test
        @DisplayName("regra de negócio ou sala inexistente: reexibe o formulário com a mensagem")
        void regraVioladaOuSalaInexistente() throws Exception {
            when(reservaService.criar(1L, "Ana", INICIO, FIM))
                    .thenThrow(new RegraNegocioException("A reserva não pode ultrapassar 8 horas."));
            when(reservaService.criar(2L, "Ana", INICIO, FIM))
                    .thenThrow(new RecursoNaoEncontradoException("Sala não encontrada: 2"));

            mvc.perform(post("/reservas").param("salaId", "1").param("solicitante", "Ana")
                            .param("inicio", "2030-05-20T14:00").param("fim", "2030-05-20T16:00"))
                    .andExpect(model().attribute("erro", "A reserva não pode ultrapassar 8 horas."));
            mvc.perform(post("/reservas").param("salaId", "2").param("solicitante", "Ana")
                            .param("inicio", "2030-05-20T14:00").param("fim", "2030-05-20T16:00"))
                    .andExpect(model().attribute("erro", "Sala não encontrada: 2"));
        }
    }

    @Nested
    @DisplayName("POST /reservas/{id}/cancelar")
    class Cancelar {

        @Test
        @DisplayName("cancela e redireciona com mensagem de sucesso")
        void cancela() throws Exception {
            mvc.perform(post("/reservas/10/cancelar"))
                    .andExpect(redirectedUrl("/reservas"))
                    .andExpect(flash().attribute("sucesso", "Reserva cancelada."));

            verify(reservaService).cancelar(10L);
        }

        @Test
        @DisplayName("reserva não ativa: redireciona com mensagem de erro")
        void cancelaReservaNaoAtiva() throws Exception {
            when(reservaService.cancelar(10L))
                    .thenThrow(new RegraNegocioException("Somente reservas ativas podem ser canceladas."));

            mvc.perform(post("/reservas/10/cancelar"))
                    .andExpect(redirectedUrl("/reservas"))
                    .andExpect(flash().attribute("erro", "Somente reservas ativas podem ser canceladas."));
        }
    }
}
