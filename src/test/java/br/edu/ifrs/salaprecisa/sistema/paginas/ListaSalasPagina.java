package br.edu.ifrs.salaprecisa.sistema.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object da tela "Salas" (lista de salas).
 */
public class ListaSalasPagina extends PaginaBase {

    public ListaSalasPagina(WebDriver driver) {
        super(driver, "Salas");
    }

    /** Abre a página inicial do sistema, que redireciona para a lista de salas. */
    public static ListaSalasPagina abrir(WebDriver driver, String urlBase) {
        driver.get(urlBase + "/");
        return new ListaSalasPagina(driver);
    }

    public FormularioSalaPagina novaSala() {
        clicarENavegar(elemento(By.id("botao-nova-sala")));
        return new FormularioSalaPagina(driver, "Nova sala");
    }

    public boolean contemSala(String nome) {
        return existe(linha(nome));
    }

    public boolean listaVazia() {
        return existe(By.id("sem-salas"));
    }

    public String tipo(String nome) {
        return celula(nome, "tipo");
    }

    public String capacidade(String nome) {
        return celula(nome, "capacidade");
    }

    /** Situação exibida para a sala: Livre, Ocupada ou Inativa. */
    public String situacao(String nome) {
        return celula(nome, "situacao");
    }

    public boolean podeReservar(String nome) {
        return !driver.findElement(linha(nome)).findElements(By.cssSelector(".link-reservar")).isEmpty();
    }

    public FormularioSalaPagina editar(String nome) {
        clicarENavegar(acao(nome, ".link-editar"));
        return new FormularioSalaPagina(driver, "Editar sala");
    }

    public FormularioReservaPagina reservar(String nome) {
        clicarENavegar(acao(nome, ".link-reservar"));
        return new FormularioReservaPagina(driver);
    }

    public ListaSalasPagina desativar(String nome) {
        clicarENavegar(acao(nome, ".botao-desativar"));
        return new ListaSalasPagina(driver);
    }

    public ListaSalasPagina ativar(String nome) {
        clicarENavegar(acao(nome, ".botao-ativar"));
        return new ListaSalasPagina(driver);
    }

    private static By linha(String nome) {
        return By.cssSelector("#tabela-salas tr[data-nome='" + nome + "']");
    }

    private String celula(String nome, String classe) {
        return elemento(linha(nome)).findElement(By.cssSelector("td." + classe)).getText().trim();
    }

    private WebElement acao(String nome, String seletor) {
        return elemento(linha(nome)).findElement(By.cssSelector(seletor));
    }
}
