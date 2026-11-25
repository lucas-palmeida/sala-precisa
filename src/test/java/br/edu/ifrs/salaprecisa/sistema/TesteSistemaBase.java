package br.edu.ifrs.salaprecisa.sistema;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import br.edu.ifrs.salaprecisa.sistema.suporte.Evidencias;
import br.edu.ifrs.salaprecisa.sistema.suporte.FabricaNavegador;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Base dos testes de sistema.
 *
 * <p>Sobe a aplicação completa em uma porta aleatória (como em produção: servidor HTTP,
 * telas, serviços e banco H2) e abre um navegador real controlado pelo Selenium, que
 * interage com o sistema do ponto de vista do usuário final. O banco é limpo antes de cada
 * teste e, se um teste falhar, uma captura de tela é salva automaticamente.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TesteSistemaBase {

    /** Dia seguinte ao da execução: o sistema não aceita reservas com início no passado. */
    protected static final LocalDate AMANHA = LocalDate.now().plusDays(1);

    protected static WebDriver driver;

    @RegisterExtension
    final TestWatcher capturaEmFalha = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext contexto, Throwable causa) {
            if (driver != null) {
                Evidencias.capturar(driver, "FALHA-" + contexto.getRequiredTestClass().getSimpleName() + "-"
                        + contexto.getRequiredTestMethod().getName());
            }
        }
    };

    @LocalServerPort
    private int porta;

    @Autowired
    protected SalaRepository salaRepository;

    @Autowired
    protected ReservaRepository reservaRepository;

    @BeforeAll
    protected static void abrirNavegador() {
        driver = FabricaNavegador.criar();
    }

    @AfterAll
    protected static void fecharNavegador() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @BeforeEach
    protected void limparBanco() {
        reservaRepository.deleteAll();
        salaRepository.deleteAll();
    }

    /** Endereço base da aplicação em execução, por exemplo http://localhost:54321. */
    protected String urlBase() {
        return "http://localhost:" + porta;
    }

    /** Cadastra uma sala diretamente no banco (preparação de cenário). */
    protected Sala salaCadastrada(String nome, int capacidade) {
        return salaRepository.save(new Sala(nome, TipoSala.SALA_REUNIAO, capacidade));
    }

    /** Data e hora de amanhã no horário informado. */
    protected static LocalDateTime amanha(int hora, int minuto) {
        return LocalDateTime.of(AMANHA, LocalTime.of(hora, minuto));
    }

    /** Salva uma captura de tela como evidência, prefixada com o nome da classe de teste. */
    protected void evidencia(String nome) {
        Evidencias.capturar(driver, getClass().getSimpleName() + "-" + nome);
    }
}
