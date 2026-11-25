package br.edu.ifrs.salaprecisa.sistema;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifrs.salaprecisa.sistema.paginas.FormularioSalaPagina;
import br.edu.ifrs.salaprecisa.sistema.paginas.ListaSalasPagina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de sistema do gerenciamento de salas (RF01, RF02 e RF03), executados em um
 * navegador real a partir das telas do sistema.
 */
@DisplayName("Sistema: gerenciamento de salas")
class GerenciarSalasSistemaIT extends TesteSistemaBase {

    @Test
    @DisplayName("Fluxo principal: usuário cadastra uma sala e a vê na lista como Livre")
    void cadastraSala() {
        ListaSalasPagina lista = ListaSalasPagina.abrir(driver, urlBase());
        assertThat(lista.listaVazia()).isTrue();

        FormularioSalaPagina formulario = lista.novaSala()
                .preencher("Auditório Central", "Auditório", "200");
        evidencia("01-cadastro-formulario-preenchido");
        lista = formulario.salvar();

        evidencia("02-cadastro-sala-na-lista");
        assertThat(lista.mensagemSucesso()).isEqualTo("Sala cadastrada com sucesso.");
        assertThat(lista.contemSala("Auditório Central")).isTrue();
        assertThat(lista.tipo("Auditório Central")).isEqualTo("Auditório");
        assertThat(lista.capacidade("Auditório Central")).isEqualTo("200");
        assertThat(lista.situacao("Auditório Central")).isEqualTo("Livre");
    }

    @Test
    @DisplayName("Fluxo alternativo: campos obrigatórios vazios exibem mensagens por campo")
    void camposObrigatorios() {
        FormularioSalaPagina formulario = ListaSalasPagina.abrir(driver, urlBase())
                .novaSala()
                .preencher("", null, "")
                .salvarEsperandoErro();

        evidencia("03-cadastro-campos-obrigatorios");
        assertThat(formulario.erroDoCampo("nome")).isEqualTo("Informe o nome da sala.");
        assertThat(formulario.erroDoCampo("tipo")).isEqualTo("Selecione o tipo da sala.");
        assertThat(formulario.erroDoCampo("capacidade")).isEqualTo("Informe a capacidade.");
        assertThat(salaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Fluxo alternativo: capacidade fora do limite é rejeitada")
    void capacidadeInvalida() {
        FormularioSalaPagina formulario = ListaSalasPagina.abrir(driver, urlBase())
                .novaSala()
                .preencher("Sala Gigante", "Outro", "1001")
                .salvarEsperandoErro();

        assertThat(formulario.erroDoCampo("capacidade")).isEqualTo("A capacidade deve ser de no máximo 1000.");
        assertThat(formulario.nome()).isEqualTo("Sala Gigante");
    }

    @Test
    @DisplayName("Fluxo alternativo: nome já cadastrado (com outra caixa) é rejeitado")
    void nomeDuplicado() {
        salaCadastrada("Lab 101", 30);

        FormularioSalaPagina formulario = ListaSalasPagina.abrir(driver, urlBase())
                .novaSala()
                .preencher("LAB 101", "Laboratório", "20")
                .salvarEsperandoErro();

        evidencia("04-cadastro-nome-duplicado");
        assertThat(formulario.mensagemErro()).isEqualTo("Já existe uma sala com o nome informado.");
        assertThat(salaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fluxo principal: usuário edita os dados de uma sala")
    void editaSala() {
        salaCadastrada("Sala 1", 8);

        ListaSalasPagina lista = ListaSalasPagina.abrir(driver, urlBase())
                .editar("Sala 1")
                .preencher("Sala 1 - Térreo", "Laboratório", "12")
                .salvar();

        assertThat(lista.mensagemSucesso()).isEqualTo("Sala atualizada com sucesso.");
        assertThat(lista.contemSala("Sala 1")).isFalse();
        assertThat(lista.tipo("Sala 1 - Térreo")).isEqualTo("Laboratório");
        assertThat(lista.capacidade("Sala 1 - Térreo")).isEqualTo("12");
    }

    @Test
    @DisplayName("Fluxo alternativo: sala desativada fica Inativa, some da reserva e pode ser reativada")
    void desativaEReativaSala() {
        salaCadastrada("Sala Ativa", 10);
        salaCadastrada("Sala Reforma", 10);

        ListaSalasPagina lista = ListaSalasPagina.abrir(driver, urlBase()).desativar("Sala Reforma");

        evidencia("05-sala-desativada");
        assertThat(lista.mensagemSucesso()).isEqualTo("Sala desativada.");
        assertThat(lista.situacao("Sala Reforma")).isEqualTo("Inativa");
        assertThat(lista.podeReservar("Sala Reforma")).isFalse();
        assertThat(lista.menuNovaReserva().salasDisponiveis())
                .containsExactly("Sala Ativa (10 pessoas)");

        lista = ListaSalasPagina.abrir(driver, urlBase()).ativar("Sala Reforma");

        assertThat(lista.situacao("Sala Reforma")).isEqualTo("Livre");
        assertThat(lista.podeReservar("Sala Reforma")).isTrue();
    }
}
