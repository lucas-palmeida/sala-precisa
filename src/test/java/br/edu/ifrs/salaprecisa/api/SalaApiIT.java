package br.edu.ifrs.salaprecisa.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Teste de componente da API REST de salas ({@code /api/salas}).
 *
 * <p>A aplicação é iniciada por completo em uma porta aleatória (servidor HTTP real,
 * serviços reais e banco H2 em memória) e exercitada "de fora", por requisições HTTP
 * feitas com RestAssured. O banco é limpo antes de cada teste.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API de salas (/api/salas)")
class SalaApiIT {

    @LocalServerPort
    private int porta;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @BeforeEach
    void configurar() {
        RestAssured.port = porta;
        RestAssured.basePath = "/api/salas";
        reservaRepository.deleteAll();
        salaRepository.deleteAll();
    }

    @AfterEach
    void restaurar() {
        RestAssured.reset();
    }

    private static String json(String nome, String tipo, Object capacidade) {
        return """
                {"nome": %s, "tipo": %s, "capacidade": %s}
                """.formatted(textoOuNulo(nome), textoOuNulo(tipo), capacidade);
    }

    private static String textoOuNulo(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    /** Cadastra uma sala pela API e devolve o id gerado. */
    private static long cadastrar(String nome, String tipo, int capacidade) {
        return given().contentType(ContentType.JSON).body(json(nome, tipo, capacidade))
                .when().post()
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Nested
    @DisplayName("POST /api/salas")
    class Cadastrar {

        @Test
        @DisplayName("201: cadastra a sala e devolve Location e corpo")
        void cadastraComSucesso() {
            given()
                .contentType(ContentType.JSON)
                .body(json("  Lab 101 ", "LABORATORIO", 30))
            .when()
                .post()
            .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/salas/\\d+$"))
                .body("id", notNullValue())
                .body("nome", equalTo("Lab 101"))
                .body("tipo", equalTo("LABORATORIO"))
                .body("capacidade", equalTo(30))
                .body("ativa", equalTo(true))
                .body("ocupada", equalTo(false));
        }

        @Test
        @DisplayName("400: lista todos os campos inválidos")
        void validaCampos() {
            given()
                .contentType(ContentType.JSON)
                .body(json("", null, 0))
            .when()
                .post()
            .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("mensagem", equalTo("Dados inválidos."))
                .body("detalhes", containsInAnyOrder(
                        "nome: O nome é obrigatório.",
                        "tipo: O tipo é obrigatório.",
                        "capacidade: A capacidade deve ser de no mínimo 1."));
        }

        @Test
        @DisplayName("400: capacidade acima do máximo (1001)")
        void capacidadeAcimaDoMaximo() {
            given().contentType(ContentType.JSON).body(json("Sala", "OUTRO", 1001))
            .when().post()
            .then()
                .statusCode(400)
                .body("detalhes", contains("capacidade: A capacidade deve ser de no máximo 1000."));
        }

        @Test
        @DisplayName("400: nome duplicado, sem diferenciar maiúsculas")
        void nomeDuplicado() {
            cadastrar("Lab 101", "LABORATORIO", 30);

            given().contentType(ContentType.JSON).body(json("LAB 101", "AUDITORIO", 50))
            .when().post()
            .then()
                .statusCode(400)
                .body("mensagem", equalTo("Já existe uma sala com o nome informado."));
        }

        @Test
        @DisplayName("400: tipo inexistente no enum")
        void tipoInexistente() {
            given().contentType(ContentType.JSON).body(json("Piscina", "PISCINA", 10))
            .when().post()
            .then()
                .statusCode(400)
                .body("mensagem", equalTo("Corpo da requisição inválido ou mal formatado."));
        }

        @Test
        @DisplayName("400: JSON mal formatado")
        void jsonMalFormatado() {
            given().contentType(ContentType.JSON).body("{\"nome\": \"Sala\", ")
            .when().post()
            .then()
                .statusCode(400)
                .body("mensagem", equalTo("Corpo da requisição inválido ou mal formatado."));
        }
    }

    @Nested
    @DisplayName("GET /api/salas")
    class Consultar {

        @Test
        @DisplayName("200: lista vazia quando não há salas")
        void listaVazia() {
            given().when().get().then().statusCode(200).body("$", empty());
        }

        @Test
        @DisplayName("200: lista as salas ordenadas por nome")
        void listaOrdenada() {
            cadastrar("Sala C", "OUTRO", 5);
            cadastrar("Sala A", "OUTRO", 5);
            cadastrar("Sala B", "OUTRO", 5);

            given().when().get()
            .then()
                .statusCode(200)
                .body("nome", contains("Sala A", "Sala B", "Sala C"));
        }

        @Test
        @DisplayName("200: busca uma sala pelo id")
        void buscaPorId() {
            long id = cadastrar("Lab 101", "LABORATORIO", 30);

            given().when().get("/{id}", id)
            .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("nome", equalTo("Lab 101"));
        }

        @Test
        @DisplayName("404: sala inexistente")
        void salaInexistente() {
            given().when().get("/{id}", 999)
            .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("mensagem", equalTo("Sala não encontrada: 999"));
        }

        @Test
        @DisplayName("400: id não numérico")
        void idInvalido() {
            given().when().get("/abc")
            .then()
                .statusCode(400)
                .body("mensagem", equalTo("Parâmetro inválido: id."));
        }

        @Test
        @DisplayName("ocupada=true quando há reserva ativa em andamento")
        void indicaSalaOcupada() {
            Sala sala = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
            Sala livre = salaRepository.save(new Sala("Lab 102", TipoSala.LABORATORIO, 30));
            LocalDateTime agora = LocalDateTime.now();
            // Inserida diretamente no banco: pela API não é possível reservar com início no passado.
            reservaRepository.save(new Reserva(sala, "Ana", agora.minusHours(1), agora.plusHours(1)));

            given().when().get("/{id}", sala.getId()).then().statusCode(200).body("ocupada", equalTo(true));
            given().when().get("/{id}", livre.getId()).then().statusCode(200).body("ocupada", equalTo(false));
            given().when().get().then().body("ocupada", contains(true, false));
        }
    }

    @Nested
    @DisplayName("PUT /api/salas/{id}")
    class Atualizar {

        @Test
        @DisplayName("200: altera os dados da sala")
        void atualiza() {
            long id = cadastrar("Sala 1", "SALA_REUNIAO", 8);

            given().contentType(ContentType.JSON).body(json("Sala 1A", "AUDITORIO", 80))
            .when().put("/{id}", id)
            .then()
                .statusCode(200)
                .body("nome", equalTo("Sala 1A"))
                .body("tipo", equalTo("AUDITORIO"))
                .body("capacidade", equalTo(80));
        }

        @Test
        @DisplayName("200: mantém o próprio nome (não é duplicidade)")
        void mantemProprioNome() {
            long id = cadastrar("Sala 1", "SALA_REUNIAO", 8);

            given().contentType(ContentType.JSON).body(json("SALA 1", "SALA_REUNIAO", 10))
            .when().put("/{id}", id)
            .then().statusCode(200).body("nome", equalTo("SALA 1"));
        }

        @Test
        @DisplayName("400: nome pertencente a outra sala")
        void nomeDeOutraSala() {
            cadastrar("Sala 1", "OUTRO", 8);
            long id = cadastrar("Sala 2", "OUTRO", 8);

            given().contentType(ContentType.JSON).body(json("sala 1", "OUTRO", 8))
            .when().put("/{id}", id)
            .then().statusCode(400).body("mensagem", equalTo("Já existe uma sala com o nome informado."));
        }

        @Test
        @DisplayName("404: sala inexistente")
        void salaInexistente() {
            given().contentType(ContentType.JSON).body(json("Sala", "OUTRO", 8))
            .when().put("/{id}", 999)
            .then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("PATCH /api/salas/{id}/desativar e /ativar")
    class DesativarAtivar {

        @Test
        @DisplayName("desativa e reativa a sala")
        void desativaEReativa() {
            long id = cadastrar("Sala 1", "OUTRO", 8);

            given().when().patch("/{id}/desativar", id).then().statusCode(200).body("ativa", equalTo(false));
            given().when().patch("/{id}/ativar", id).then().statusCode(200).body("ativa", equalTo(true));
        }

        @Test
        @DisplayName("404: desativar sala inexistente")
        void desativaInexistente() {
            given().when().patch("/{id}/desativar", 999).then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("GET /api/salas/{id}/reservas")
    class ReservasDaSala {

        @Test
        @DisplayName("200: lista apenas as reservas da sala, em ordem de início")
        void listaReservasDaSala() {
            Sala sala = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
            Sala outra = salaRepository.save(new Sala("Lab 102", TipoSala.LABORATORIO, 30));
            LocalDateTime amanha = LocalDateTime.now().plusDays(1).withNano(0);
            reservaRepository.save(new Reserva(sala, "Tarde", amanha.withHour(14), amanha.withHour(15)));
            reservaRepository.save(new Reserva(outra, "Outra", amanha.withHour(9), amanha.withHour(10)));
            reservaRepository.save(new Reserva(sala, "Manhã", amanha.withHour(8), amanha.withHour(9)));

            given().when().get("/{id}/reservas", sala.getId())
            .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("solicitante", contains("Manhã", "Tarde"));
        }

        @Test
        @DisplayName("404: sala inexistente")
        void salaInexistente() {
            given().when().get("/{id}/reservas", 999).then().statusCode(404);
        }
    }
}
