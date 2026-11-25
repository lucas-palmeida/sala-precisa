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
 *   <li>{@code navegador}: {@code edge} (padrão), {@code chrome} ou {@code firefox};</li>
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
        String navegador = System.getProperty("navegador", "edge").toLowerCase(Locale.ROOT);
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
}
