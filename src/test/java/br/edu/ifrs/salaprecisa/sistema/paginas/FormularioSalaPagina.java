package br.edu.ifrs.salaprecisa.sistema.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

/**
 * Page Object do formulário de cadastro/edição de sala.
 */
public class FormularioSalaPagina extends PaginaBase {

    private final String titulo;

    public FormularioSalaPagina(WebDriver driver, String titulo) {
        super(driver, titulo);
        this.titulo = titulo;
    }

    /**
     * Preenche o formulário.
     *
     * @param nome       nome da sala (vazio para deixar em branco)
     * @param tipo       descrição visível do tipo (ex.: "Auditório"); {@code null} = "Selecione..."
     * @param capacidade capacidade em texto (vazio para deixar em branco)
     */
    public FormularioSalaPagina preencher(String nome, String tipo, String capacidade) {
        digitar(By.id("nome"), nome);
        Select select = new Select(elemento(By.id("tipo")));
        if (tipo == null) {
            select.selectByValue("");
        } else {
            select.selectByVisibleText(tipo);
        }
        digitar(By.id("capacidade"), capacidade);
        return this;
    }

    public String nome() {
        return elemento(By.id("nome")).getDomProperty("value");
    }

    /** Envia o formulário esperando sucesso (volta para a lista de salas). */
    public ListaSalasPagina salvar() {
        clicarENavegar(elemento(By.id("botao-salvar")));
        return new ListaSalasPagina(driver);
    }

    /** Envia o formulário esperando erro (o próprio formulário é reexibido). */
    public FormularioSalaPagina salvarEsperandoErro() {
        clicarENavegar(elemento(By.id("botao-salvar")));
        return new FormularioSalaPagina(driver, titulo);
    }
}
