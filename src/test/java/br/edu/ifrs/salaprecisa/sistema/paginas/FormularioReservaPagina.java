package br.edu.ifrs.salaprecisa.sistema.paginas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Page Object do formulário "Nova reserva".
 */
public class FormularioReservaPagina extends PaginaBase {

    /** Formato do campo HTML datetime-local. */
    private static final DateTimeFormatter FORMATO_CAMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public FormularioReservaPagina(WebDriver driver) {
        super(driver, "Nova reserva");
    }

    /** Seleciona a sala pelo nome (as opções têm o formato "Nome (N pessoas)"). */
    public FormularioReservaPagina selecionarSala(String nomeSala) {
        Select select = new Select(elemento(By.id("salaId")));
        for (WebElement opcao : select.getOptions()) {
            if (opcao.getText().startsWith(nomeSala + " (")) {
                opcao.click();
                return this;
            }
        }
        throw new IllegalArgumentException("Sala não disponível para seleção: " + nomeSala);
    }

    public String salaSelecionada() {
        return new Select(elemento(By.id("salaId"))).getFirstSelectedOption().getText();
    }

    /** Textos das salas disponíveis para seleção (sem a opção "Selecione..."). */
    public List<String> salasDisponiveis() {
        return new Select(elemento(By.id("salaId"))).getOptions().stream()
                .map(WebElement::getText)
                .filter(texto -> !texto.startsWith("Selecione"))
                .toList();
    }

    public FormularioReservaPagina solicitante(String nome) {
        digitar(By.id("solicitante"), nome);
        return this;
    }

    public FormularioReservaPagina periodo(LocalDateTime inicio, LocalDateTime fim) {
        definirValor(elemento(By.id("inicio")), inicio == null ? "" : FORMATO_CAMPO.format(inicio));
        definirValor(elemento(By.id("fim")), fim == null ? "" : FORMATO_CAMPO.format(fim));
        return this;
    }

    /** Envia esperando sucesso (vai para a lista de reservas). */
    public ListaReservasPagina reservar() {
        clicarENavegar(elemento(By.id("botao-reservar")));
        return new ListaReservasPagina(driver);
    }

    /** Envia esperando erro (o formulário é reexibido). */
    public FormularioReservaPagina reservarEsperandoErro() {
        clicarENavegar(elemento(By.id("botao-reservar")));
        return new FormularioReservaPagina(driver);
    }
}
