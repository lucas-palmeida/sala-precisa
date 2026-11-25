package br.edu.ifrs.salaprecisa.sistema.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object da tela "Reservas" (lista de reservas).
 */
public class ListaReservasPagina extends PaginaBase {

    public ListaReservasPagina(WebDriver driver) {
        super(driver, "Reservas");
    }

    public static ListaReservasPagina abrir(WebDriver driver, String urlBase) {
        driver.get(urlBase + "/reservas");
        return new ListaReservasPagina(driver);
    }

    public boolean contemReserva(String solicitante) {
        return existe(linha(solicitante));
    }

    public int quantidade() {
        return driver.findElements(By.cssSelector("#tabela-reservas tbody tr")).size();
    }

    public boolean listaVazia() {
        return existe(By.id("sem-reservas"));
    }

    public String sala(String solicitante) {
        return celula(solicitante, "sala");
    }

    public String inicio(String solicitante) {
        return celula(solicitante, "inicio");
    }

    public String fim(String solicitante) {
        return celula(solicitante, "fim");
    }

    /** Status exibido: Ativa, Cancelada ou Encerrada. */
    public String status(String solicitante) {
        return celula(solicitante, "status");
    }

    public boolean podeCancelar(String solicitante) {
        return !driver.findElement(linha(solicitante)).findElements(By.cssSelector(".botao-cancelar")).isEmpty();
    }

    public ListaReservasPagina cancelar(String solicitante) {
        clicarENavegar(elemento(linha(solicitante)).findElement(By.cssSelector(".botao-cancelar")));
        return new ListaReservasPagina(driver);
    }

    private static By linha(String solicitante) {
        return By.xpath("//table[@id='tabela-reservas']//tr[td[@class='solicitante' and normalize-space()='"
                + solicitante + "']]");
    }

    private String celula(String solicitante, String classe) {
        return elemento(linha(solicitante)).findElement(By.cssSelector("td." + classe)).getText().trim();
    }
}
