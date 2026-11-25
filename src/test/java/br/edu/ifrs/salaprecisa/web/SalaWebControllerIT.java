package br.edu.ifrs.salaprecisa.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de componente da camada web de salas (controller + templates Thymeleaf).
 *
 * <p>O {@link WebMvcTest} carrega apenas a infraestrutura do Spring MVC e o controller
 * testado. O {@link SalaService} é substituído por um mock ({@link MockitoBean}), isolando
 * o componente web das regras de negócio e do banco. As requisições são simuladas com
 * {@link MockMvc}, sem servidor HTTP real, e o HTML gerado é verificado.</p>
 */
@WebMvcTest(SalaWebController.class)
@DisplayName("Telas de salas (componente web)")
class SalaWebControllerIT {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SalaService salaService;

    private static Sala sala(long id, String nome, boolean ativa) {
        Sala sala = new Sala(nome, TipoSala.LABORATORIO, 30);
        ReflectionTestUtils.setField(sala, "id", id);
        if (!ativa) {
            sala.desativar();
        }
        return sala;
    }

    @Test
    @DisplayName("GET / redireciona para a lista de salas")
    void raizRedirecionaParaSalas() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"));
    }

    @Nested
    @DisplayName("GET /salas")
    class Listar {

        @Test
        @DisplayName("exibe as salas com a situação Livre, Ocupada ou Inativa")
        void exibeSituacaoDasSalas() throws Exception {
            when(salaService.listar()).thenReturn(List.of(
                    sala(1, "Lab Livre", true), sala(2, "Lab Ocupado", true), sala(3, "Lab Inativo", false)));
            when(salaService.idsSalasOcupadasAgora()).thenReturn(Set.of(2L));

            mvc.perform(get("/salas"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("salas/lista"))
                    .andExpect(content().string(containsString("Lab Livre")))
                    .andExpect(content().string(containsString("etiqueta livre")))
                    .andExpect(content().string(containsString("etiqueta ocupada")))
                    .andExpect(content().string(containsString("etiqueta inativa")))
                    .andExpect(content().string(containsString("Laboratório")));
        }

        @Test
        @DisplayName("exibe mensagem quando não há salas")
        void listaVazia() throws Exception {
            when(salaService.listar()).thenReturn(List.of());
            when(salaService.idsSalasOcupadasAgora()).thenReturn(Set.of());

            mvc.perform(get("/salas"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Nenhuma sala cadastrada.")));
        }
    }

    @Nested
    @DisplayName("cadastro (GET /salas/nova e POST /salas)")
    class Cadastro {

        @Test
        @DisplayName("exibe o formulário vazio com os tipos de sala")
        void exibeFormulario() throws Exception {
            mvc.perform(get("/salas/nova"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("salas/form"))
                    .andExpect(model().attributeExists("salaForm", "tipos"))
                    .andExpect(model().attribute("acao", "/salas"))
                    .andExpect(content().string(containsString("Auditório")));
        }

        @Test
        @DisplayName("dados válidos: cadastra e redireciona com mensagem de sucesso")
        void cadastraComSucesso() throws Exception {
            mvc.perform(post("/salas")
                            .param("nome", "Lab 101")
                            .param("tipo", "LABORATORIO")
                            .param("capacidade", "30"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/salas"))
                    .andExpect(flash().attribute("sucesso", "Sala cadastrada com sucesso."));

            verify(salaService).cadastrar("Lab 101", TipoSala.LABORATORIO, 30);
        }

        @Test
        @DisplayName("campos vazios: reexibe o formulário com os erros e não chama o serviço")
        void camposVazios() throws Exception {
            mvc.perform(post("/salas").param("nome", "").param("tipo", "").param("capacidade", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("salas/form"))
                    .andExpect(model().attributeHasFieldErrors("salaForm", "nome", "tipo", "capacidade"))
                    .andExpect(content().string(containsString("Informe o nome da sala.")));

            verify(salaService, never()).cadastrar(anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("regra de negócio violada: reexibe o formulário com a mensagem do serviço")
        void nomeDuplicado() throws Exception {
            when(salaService.cadastrar("Lab 101", TipoSala.LABORATORIO, 30))
                    .thenThrow(new RegraNegocioException("Já existe uma sala com o nome informado."));

            mvc.perform(post("/salas")
                            .param("nome", "Lab 101")
                            .param("tipo", "LABORATORIO")
                            .param("capacidade", "30"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("salas/form"))
                    .andExpect(model().attribute("erro", "Já existe uma sala com o nome informado."))
                    .andExpect(content().string(containsString("id=\"mensagem-erro\"")));
        }
    }

    @Nested
    @DisplayName("edição (GET /salas/{id}/editar e POST /salas/{id})")
    class Edicao {

        @Test
        @DisplayName("exibe o formulário preenchido com os dados da sala")
        void exibeFormularioPreenchido() throws Exception {
            when(salaService.buscarPorId(1L)).thenReturn(sala(1, "Lab 101", true));

            mvc.perform(get("/salas/1/editar"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("salaForm", hasProperty("nome", is("Lab 101"))))
                    .andExpect(model().attribute("acao", "/salas/1"))
                    .andExpect(content().string(containsString("value=\"Lab 101\"")));
        }

        @Test
        @DisplayName("sala inexistente: exibe página de erro com status 404")
        void salaInexistente() throws Exception {
            when(salaService.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Sala não encontrada: 99"));

            mvc.perform(get("/salas/99/editar"))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("erro"))
                    .andExpect(content().string(containsString("Sala não encontrada: 99")));
        }

        @Test
        @DisplayName("id inválido na URL: exibe página de erro com status 400")
        void idInvalido() throws Exception {
            mvc.perform(get("/salas/abc/editar"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("erro"))
                    .andExpect(content().string(containsString("Endereço inválido.")));
        }

        @Test
        @DisplayName("dados válidos: atualiza e redireciona")
        void atualiza() throws Exception {
            mvc.perform(post("/salas/1").param("nome", "Lab 1A").param("tipo", "AUDITORIO").param("capacidade", "40"))
                    .andExpect(redirectedUrl("/salas"))
                    .andExpect(flash().attribute("sucesso", "Sala atualizada com sucesso."));

            verify(salaService).atualizar(1L, "Lab 1A", TipoSala.AUDITORIO, 40);
        }

        @Test
        @DisplayName("dados inválidos: reexibe o formulário de edição")
        void dadosInvalidos() throws Exception {
            mvc.perform(post("/salas/1").param("nome", "Lab").param("tipo", "OUTRO").param("capacidade", "0"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("salaForm", "capacidade"))
                    .andExpect(model().attribute("titulo", "Editar sala"));

            verify(salaService, never()).atualizar(anyLong(), anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("regra de negócio violada: reexibe o formulário com a mensagem")
        void regraVioladaNaEdicao() throws Exception {
            when(salaService.atualizar(1L, "Lab 2", TipoSala.OUTRO, 10))
                    .thenThrow(new RegraNegocioException("Já existe uma sala com o nome informado."));

            mvc.perform(post("/salas/1").param("nome", "Lab 2").param("tipo", "OUTRO").param("capacidade", "10"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("erro", "Já existe uma sala com o nome informado."));
        }
    }

    @Nested
    @DisplayName("desativar / ativar")
    class DesativarAtivar {

        @Test
        @DisplayName("POST /salas/{id}/desativar aciona o serviço e redireciona")
        void desativa() throws Exception {
            mvc.perform(post("/salas/4/desativar"))
                    .andExpect(redirectedUrl("/salas"))
                    .andExpect(flash().attribute("sucesso", "Sala desativada."));

            verify(salaService).desativar(4L);
        }

        @Test
        @DisplayName("POST /salas/{id}/ativar aciona o serviço e redireciona")
        void ativa() throws Exception {
            mvc.perform(post("/salas/4/ativar"))
                    .andExpect(redirectedUrl("/salas"))
                    .andExpect(flash().attribute("sucesso", "Sala reativada."));

            verify(salaService).ativar(4L);
        }
    }
}
