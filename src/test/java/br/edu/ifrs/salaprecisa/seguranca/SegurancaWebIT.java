package br.edu.ifrs.salaprecisa.seguranca;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Testes de segurança da interface web: XSS (armazenado e refletido), cabeçalhos
 * de segurança HTTP e proteção do identificador de sessão.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Segurança: interface web")
class SegurancaWebIT {

    @LocalServerPort
    private int porta;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @BeforeEach
    void configurar() {
        RestAssured.port = porta;
        reservaRepository.deleteAll();
        salaRepository.deleteAll();
    }

    @AfterEach
    void restaurar() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Cross-Site Scripting (XSS)")
    class Xss {

        @Test
        @DisplayName("XSS armazenado: nome de sala com <script> é exibido escapado na lista de salas")
        void xssArmazenadoNaListaDeSalas() {
            salaRepository.save(new Sala("<script>alert('xss')</script>", TipoSala.OUTRO, 5));

            given().when().get("/salas")
                .then()
                .statusCode(200)
                .body(not(containsString("<script>alert")))
                .body(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        }

        @Test
        @DisplayName("XSS armazenado: solicitante com <img onerror> é exibido escapado na lista de reservas")
        void xssArmazenadoNaListaDeReservas() {
            Sala sala = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
            LocalDateTime amanha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            reservaRepository.save(new Reserva(sala, "<img src=x onerror=alert(1)>", amanha, amanha.plusHours(1)));

            given().when().get("/reservas")
                .then()
                .statusCode(200)
                .body(not(containsString("<img src=x")))
                .body(containsString("&lt;img src=x onerror=alert(1)&gt;"));
        }

        @Test
        @DisplayName("XSS refletido: valor malicioso reexibido no formulário com erro é escapado")
        void xssRefletidoNoFormulario() {
            given()
                .formParam("nome", "\"><script>alert(1)</script>")
                .formParam("tipo", "OUTRO")
                .formParam("capacidade", "")
            .when().post("/salas")
            .then()
                .statusCode(200)
                .body(not(containsString("<script>alert(1)</script>")))
                .body(containsString("&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;"));
        }
    }

    @Nested
    @DisplayName("Cabeçalhos de segurança HTTP")
    class Cabecalhos {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/salas", "/reservas/nova", "/api/salas", "/css/estilo.css"})
        @DisplayName("respostas incluem os cabeçalhos de proteção do navegador")
        void cabecalhosPresentes(String caminho) {
            given().when().get(caminho)
                .then()
                .statusCode(200)
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .header("X-Frame-Options", equalTo("DENY"))
                .header("Referrer-Policy", equalTo("same-origin"))
                .header("Content-Security-Policy", notNullValue())
                .header("Content-Security-Policy", containsString("default-src 'self'"))
                .header("Content-Security-Policy", containsString("frame-ancestors 'none'"));
        }
    }

    @Nested
    @DisplayName("Identificador de sessão")
    class Sessao {

        @Test
        @DisplayName("redirecionamento após cadastro não expõe o jsessionid na URL")
        void sessaoNaoApareceNaUrl() {
            given()
                .redirects().follow(false)
                .formParam("nome", "Sala Nova")
                .formParam("tipo", "OUTRO")
                .formParam("capacidade", "10")
            .when().post("/salas")
            .then()
                .statusCode(302)
                .header("Location", not(containsString("jsessionid")));
        }

        @Test
        @DisplayName("cookie de sessão é HttpOnly e SameSite=Lax")
        void cookieDeSessaoProtegido() {
            given()
                .redirects().follow(false)
                .formParam("nome", "Sala Cookie")
                .formParam("tipo", "OUTRO")
                .formParam("capacidade", "10")
            .when().post("/salas")
            .then()
                .statusCode(302)
                .header("Set-Cookie", containsString("JSESSIONID="))
                .header("Set-Cookie", containsString("HttpOnly"))
                .header("Set-Cookie", containsString("SameSite=Lax"));
        }
    }
}
