package br.edu.ifrs.salaprecisa.sistema.suporte;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Salva capturas de tela como evidência de execução dos testes de sistema,
 * no diretório {@code docs/evidencias/sistema}.
 */
public final class Evidencias {

    private static final Path DIRETORIO = Path.of("docs", "evidencias", "sistema");

    private Evidencias() {
    }

    public static void capturar(WebDriver driver, String nome) {
        try {
            Files.createDirectories(DIRETORIO);
            Path origem = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(origem, DIRETORIO.resolve(nome + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível salvar a captura de tela " + nome, ex);
        }
    }
}
