package br.edu.ifrs.salaprecisa.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.sistema.TesteSistemaBase;
import br.edu.ifrs.salaprecisa.sistema.paginas.ListaReservasPagina;
import br.edu.ifrs.salaprecisa.sistema.paginas.ListaSalasPagina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;

/**
 * Teste de segurança executado em navegador real (perfil "sistema"): verifica que
 * conteúdo malicioso armazenado no banco é exibido como texto e <b>não é executado</b>
 * pelo navegador.
 *
 * <p>Cada payload, se executado, definiria a variável global {@code window.xss}.
 * O teste confirma que ela continua indefinida e que nenhum alerta foi aberto.</p>
 */
@DisplayName("Segurança: XSS no navegador")
class XssNavegadorSistemaIT extends TesteSistemaBase {

    private static final String SCRIPT = "<script>window.xss='script'</script>";
    private static final String IMG = "<img src=x onerror=\"window.xss='img'\">";
    private static final String SVG = "<svg onload=\"window.xss='svg'\">";

    private boolean scriptFoiExecutado() {
        Object valor = ((JavascriptExecutor) driver).executeScript("return typeof window.xss !== 'undefined';");
        return Boolean.TRUE.equals(valor);
    }

    @Test
    @DisplayName("payloads XSS armazenados são exibidos como texto e não executam")
    void xssArmazenadoNaoExecuta() {
        Sala salaScript = salaRepository.save(new Sala(SCRIPT, TipoSala.OUTRO, 5));
        salaRepository.save(new Sala(IMG, TipoSala.OUTRO, 5));
        reservaRepository.save(new Reserva(salaScript, SVG, amanha(10, 0), amanha(11, 0)));

        ListaSalasPagina salas = ListaSalasPagina.abrir(driver, urlBase());
        evidencia("01-xss-lista-de-salas");
        String textoTabelaSalas = driver.findElement(By.id("tabela-salas")).getText();
        assertThat(textoTabelaSalas).contains(SCRIPT, IMG);
        assertThat(scriptFoiExecutado()).isFalse();
        assertThatThrownBy(() -> driver.switchTo().alert()).isInstanceOf(NoAlertPresentException.class);

        ListaReservasPagina reservas = salas.menuReservas();
        evidencia("02-xss-lista-de-reservas");
        // O texto da tabela é verificado diretamente: as aspas dos payloads inviabilizam seletores XPath.
        assertThat(reservas.quantidade()).isEqualTo(1);
        assertThat(driver.findElement(By.id("tabela-reservas")).getText()).contains(SVG, SCRIPT);
        assertThat(scriptFoiExecutado()).isFalse();
    }
}
