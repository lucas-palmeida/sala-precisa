package br.edu.ifrs.salaprecisa.seguranca;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import org.hamcrest.Matcher;
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
 * Testes de segurança da API REST: injeção de SQL, entradas abusivas,
 * atribuição em massa e exposição de informações internas.
 *
 * <p>Cada teste descreve o comportamento <b>seguro esperado</b>. Um teste que falha
 * indica uma vulnerabilidade (os achados estão registrados em docs/teste-seguranca.md).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Segurança: API REST")
class SegurancaApiIT {

    private static final String AMANHA = LocalDate.now().plusDays(1).toString();

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

    /** Converte um texto em literal JSON, escapando aspas e barras invertidas. */
    private static String texto(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static Response postarSala(String nome) {
        return given().contentType(ContentType.JSON)
                .body("{\"nome\": " + texto(nome) + ", \"tipo\": \"OUTRO\", \"capacidade\": 10}")
                .when().post("/api/salas");
    }

    /** Nenhum destes trechos deve aparecer em respostas de erro (indicam vazamento de detalhes internos). */
    private static Matcher<String> semDetalhesInternos() {
        return allOf(
                not(containsString("Exception")),
                not(containsString("trace")),
                not(containsString("at org.")),
                not(containsString("java.")),
                not(containsString("org.springframework")),
                not(containsString("org.hibernate")),
                not(containsString("SQL")));
    }

    @Nested
    @DisplayName("Injeção de SQL")
    class InjecaoSql {

        @ParameterizedTest(name = "payload: {0}")
        @ValueSource(strings = {
            "'; DROP TABLE sala; --",
            "' OR '1'='1",
            "\" OR \"1\"=\"1",
            "1; DELETE FROM reserva",
            "Lab' UNION SELECT * FROM sala --"
        })
        @DisplayName("payload é armazenado como texto literal e o banco permanece íntegro")
        void payloadArmazenadoComoTexto(String payload) {
            salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));

            postarSala(payload).then().statusCode(201).body("nome", equalTo(payload));

            given().when().get("/api/salas")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("nome", hasItems("Lab 101", payload));
        }

        @Test
        @DisplayName("consulta de nome duplicado não é manipulável por ' OR '1'='1")
        void consultaDeDuplicidadeNaoEhInjetavel() {
            salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));

            // Se a consulta fosse montada por concatenação, a condição seria sempre verdadeira
            // e o sistema responderia "nome duplicado" (400).
            postarSala("x' OR '1'='1").then().statusCode(201);
        }

        @ParameterizedTest(name = "id: {0}")
        @ValueSource(strings = {"1 OR 1=1", "1' OR '1'='1", "1 UNION SELECT 1"})
        @DisplayName("identificador com SQL na URL é rejeitado como parâmetro inválido")
        void idComSql(String id) {
            given().when().get("/api/salas/{id}", id)
                .then()
                .statusCode(400)
                .body("mensagem", equalTo("Parâmetro inválido: id."));
        }

        @Test
        @DisplayName("solicitante com SQL é armazenado literalmente na reserva")
        void solicitanteComSql() {
            Sala sala = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
            String payload = "Ana'); UPDATE reserva SET status='CANCELADA'; --";

            given().contentType(ContentType.JSON)
                .body("""
                    {"salaId": %d, "solicitante": %s, "inicio": "%sT10:00:00", "fim": "%sT11:00:00"}
                    """.formatted(sala.getId(), texto(payload), AMANHA, AMANHA))
            .when().post("/api/reservas")
            .then()
                .statusCode(201)
                .body("solicitante", equalTo(payload))
                .body("status", equalTo("ATIVA"));
        }
    }

    @Nested
    @DisplayName("Entradas abusivas e atribuição em massa")
    class EntradasAbusivas {

        @Test
        @DisplayName("nome com 100 caracteres é aceito e com 101 é rejeitado (limite)")
        void limiteDeTamanhoDoNome() {
            postarSala("A".repeat(100)).then().statusCode(201);
            postarSala("B".repeat(101)).then()
                .statusCode(400)
                .body("detalhes", hasItem("nome: O nome deve ter no máximo 100 caracteres."));
        }

        @Test
        @DisplayName("solicitante com 10.000 caracteres é rejeitado")
        void solicitanteGigante() {
            Sala sala = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));

            given().contentType(ContentType.JSON)
                .body("""
                    {"salaId": %d, "solicitante": "%s", "inicio": "%sT10:00:00", "fim": "%sT11:00:00"}
                    """.formatted(sala.getId(), "x".repeat(10_000), AMANHA, AMANHA))
            .when().post("/api/reservas")
            .then()
                .statusCode(400)
                .body("detalhes", hasItem("solicitante: O solicitante deve ter no máximo 100 caracteres."));
            assertThat(reservaRepository.count()).isZero();
        }

        @Test
        @DisplayName("capacidade acima do limite de int (overflow) é rejeitada")
        void capacidadeOverflow() {
            given().contentType(ContentType.JSON)
                .body("{\"nome\": \"Sala\", \"tipo\": \"OUTRO\", \"capacidade\": 99999999999}")
            .when().post("/api/salas")
            .then().statusCode(400).body(semDetalhesInternos());
        }

        @Test
        @DisplayName("ids negativos ou muito grandes não causam erro interno")
        void idsExtremos() {
            given().when().get("/api/salas/-1").then().statusCode(404);
            given().when().get("/api/salas/99999999999999999999").then().statusCode(400);
        }

        @Test
        @DisplayName("corpo vazio é rejeitado com 400")
        void corpoVazio() {
            given().contentType(ContentType.JSON).body("")
            .when().post("/api/salas")
            .then().statusCode(400).body(semDetalhesInternos());
        }

        @Test
        @DisplayName("tipo de conteúdo não suportado é rejeitado com 415, sem detalhes internos")
        void tipoDeConteudoNaoSuportado() {
            given().contentType(ContentType.TEXT).body("nome=Sala")
            .when().post("/api/salas")
            .then().statusCode(415).body(semDetalhesInternos());
        }

        @Test
        @DisplayName("método HTTP não suportado é rejeitado com 405")
        void metodoNaoSuportado() {
            given().when().delete("/api/salas/1").then().statusCode(405).body(semDetalhesInternos());
        }

        @Test
        @DisplayName("atribuição em massa: id e ativa enviados pelo cliente são ignorados")
        void atribuicaoEmMassaSala() {
            given().contentType(ContentType.JSON)
                .body("{\"id\": 999, \"nome\": \"Sala X\", \"tipo\": \"OUTRO\", \"capacidade\": 5, \"ativa\": false}")
            .when().post("/api/salas")
            .then()
                .statusCode(201)
                .body("id", not(equalTo(999)))
                .body("ativa", equalTo(true));
        }

        @Test
        @DisplayName("atribuição em massa: status enviado pelo cliente é ignorado na reserva")
        void atribuicaoEmMassaReserva() {
            Sala sala = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));

            given().contentType(ContentType.JSON)
                .body("""
                    {"salaId": %d, "solicitante": "Ana", "inicio": "%sT10:00:00", "fim": "%sT11:00:00",
                     "status": "ENCERRADA", "id": 555}
                    """.formatted(sala.getId(), AMANHA, AMANHA))
            .when().post("/api/reservas")
            .then()
                .statusCode(201)
                .body("status", equalTo("ATIVA"))
                .body("id", not(equalTo(555)));
        }
    }

    @Nested
    @DisplayName("Exposição de informações")
    class ExposicaoDeInformacoes {

        @Test
        @DisplayName("JSON mal formado não ecoa detalhes do parser")
        void jsonMalFormado() {
            given().contentType(ContentType.JSON).body("{\"nome\": \"Sala\", \"capacidade\": [}")
            .when().post("/api/salas")
            .then()
                .statusCode(400)
                .body(semDetalhesInternos())
                .body(not(containsString("line:")))
                .body(not(containsString("column:")));
        }

        @Test
        @DisplayName("rota inexistente devolve 404 sem stack trace nem mensagem interna")
        void rotaInexistente() {
            given().when().get("/api/nao-existe")
                .then()
                .statusCode(404)
                .body(semDetalhesInternos())
                .body("trace", nullValue())
                .body("exception", nullValue());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"/h2-console", "/h2-console/login.do", "/actuator", "/actuator/env"})
        @DisplayName("consoles e endpoints administrativos não estão expostos")
        void consolesAdministrativosInacessiveis(String caminho) {
            given().redirects().follow(false).when().get(caminho).then().statusCode(404);
        }

        @Test
        @DisplayName("respostas não identificam o servidor nem a tecnologia")
        void semIdentificacaoDoServidor() {
            given().when().get("/api/salas")
                .then()
                .statusCode(200)
                .header("Server", nullValue())
                .header("X-Powered-By", nullValue());
        }
    }
}
