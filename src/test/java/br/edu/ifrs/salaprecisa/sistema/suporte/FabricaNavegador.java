package br.edu.ifrs.salaprecisa.sistema.suporte;

import java.util.Locale;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Cria o {@link WebDriver} conforme as propriedades de sistema:
 * <ul>
 *   <li>{@code navegador}: {@code auto} (padrão), {@code edge}, {@code chrome} ou {@code firefox}.
 *       Em {@code auto}, usa o Microsoft Edge no Windows e o Google Chrome no Linux e no macOS;</li>
 *   <li>{@code headless}: {@code true} (padrão) executa sem abrir janela.</li>
 * </ul>
 * O driver do navegador (msedgedriver, chromedriver, geckodriver) é obtido automaticamente
 * pelo Selenium Manager, incluído no Selenium 4.
 */
public final class FabricaNavegador {

    private static final String TAMANHO_JANELA = "--window-size=1366,900";

    private FabricaNavegador() {
    }

    public static WebDriver criar() {
        String navegador = System.getProperty("navegador", "auto").toLowerCase(Locale.ROOT);
        if ("auto".equals(navegador)) {
            navegador = navegadorPadraoDoSistema();
        }
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        return switch (navegador) {
            case "chrome" -> {
                ChromeOptions opcoes = new ChromeOptions();
                opcoes.addArguments(TAMANHO_JANELA);
                if (headless) {
                    opcoes.addArguments("--headless=new");
                }
                yield new ChromeDriver(opcoes);
            }
            case "firefox" -> {
                FirefoxOptions opcoes = new FirefoxOptions();
                opcoes.addArguments("-width=1366", "-height=900");
                if (headless) {
                    opcoes.addArguments("-headless");
                }
                yield new FirefoxDriver(opcoes);
            }
            default -> {
                EdgeOptions opcoes = new EdgeOptions();
                opcoes.addArguments(TAMANHO_JANELA);
                if (headless) {
                    opcoes.addArguments("--headless=new");
                }
                yield new EdgeDriver(opcoes);
            }
        };
    }

    /**
     * Navegador usado quando nenhum é informado: o Edge acompanha o Windows; no Linux e no
     * macOS o Chrome é o mais comum (e, se não estiver instalado, o Selenium Manager baixa
     * automaticamente uma versão própria para testes, o Chrome for Testing).
     */
    static String navegadorPadraoDoSistema() {
        String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return sistema.contains("win") ? "edge" : "chrome";
    }
}
