package br.edu.ifrs.salaprecisa.sistema.paginas;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Comportamento comum a todos os Page Objects.
 *
 * <p>Padrão Page Object: cada tela do sistema é representada por uma classe que expõe
 * as ações do usuário (preencher, salvar, cancelar...) e esconde os detalhes de
 * localização dos elementos HTML. Assim os testes ficam legíveis e, se o HTML mudar,
 * apenas o Page Object precisa ser ajustado.</p>
 */
public abstract class PaginaBase {

    private static final Duration TEMPO_MAXIMO = Duration.ofSeconds(10);

    protected final WebDriver driver;
    protected final WebDriverWait espera;

    protected PaginaBase(WebDriver driver, String tituloEsperado) {
        this.driver = driver;
        this.espera = new WebDriverWait(driver, TEMPO_MAXIMO);
        espera.until(ExpectedConditions.titleIs(tituloEsperado + " | SalaPrecisa"));
    }

    /** Aguarda o elemento ficar visível e o devolve. */
    protected WebElement elemento(By localizador) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(localizador));
    }

    protected boolean existe(By localizador) {
        return !driver.findElements(localizador).isEmpty();
    }

    /** Clica em um elemento que provoca navegação e aguarda a página atual ser substituída. */
    protected void clicarENavegar(WebElement elemento) {
        WebElement html = driver.findElement(By.tagName("html"));
        elemento.click();
        espera.until(ExpectedConditions.stalenessOf(html));
    }

    /**
     * Define o valor de um campo via JavaScript. Usado nos campos {@code datetime-local},
     * cuja digitação varia conforme o idioma e o navegador.
     */
    protected void definirValor(WebElement campo, String valor) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", campo, valor);
    }

    protected void digitar(By localizador, String texto) {
        WebElement campo = elemento(localizador);
        campo.clear();
        if (texto != null) {
            campo.sendKeys(texto);
        }
    }

    public String mensagemSucesso() {
        return elemento(By.id("mensagem-sucesso")).getText();
    }

    public String mensagemErro() {
        return elemento(By.id("mensagem-erro")).getText();
    }

    public boolean temMensagemErro() {
        return existe(By.id("mensagem-erro"));
    }

    /** Texto do erro de validação exibido abaixo do campo informado. */
    public String erroDoCampo(String campo) {
        return elemento(By.id("erro-" + campo)).getText();
    }

    public boolean campoTemErro(String campo) {
        return existe(By.id("erro-" + campo));
    }

    public ListaSalasPagina menuSalas() {
        clicarENavegar(elemento(By.id("menu-salas")));
        return new ListaSalasPagina(driver);
    }

    public ListaReservasPagina menuReservas() {
        clicarENavegar(elemento(By.id("menu-reservas")));
        return new ListaReservasPagina(driver);
    }

    public FormularioReservaPagina menuNovaReserva() {
        clicarENavegar(elemento(By.id("menu-nova-reserva")));
        return new FormularioReservaPagina(driver);
    }
}
