package br.edu.ifrs.salaprecisa.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.repositorio.ReservaRepository;
import br.edu.ifrs.salaprecisa.repositorio.SalaRepository;
import br.edu.ifrs.salaprecisa.servico.EncerramentoReservasJob;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Teste de componente da API REST de reservas ({@code /api/reservas}).
 *
 * <p>Exercita, pela interface HTTP, as regras de negócio centrais do sistema: criação,
 * detecção de conflito de horário, validações de período, cancelamento e encerramento
 * automático. A aplicação roda por completo (servidor, serviços e banco H2).</p>
 *
 * <p>As datas são relativas ao dia seguinte ao da execução ({@code AMANHA}), pois o
 * sistema não aceita reservas com início no passado.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API de reservas (/api/reservas)")
class ReservaApiIT {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final LocalDateTime AMANHA = LocalDateTime.now().plusDays(1)
            .withHour(0).withMinute(0).withSecond(0).withNano(0);

    @LocalServerPort
    private int porta;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EncerramentoReservasJob encerramentoJob;

    private Sala lab;
    private Sala auditorio;

    @BeforeEach
    void configurar() {
        RestAssured.port = porta;
        RestAssured.basePath = "/api/reservas";
        reservaRepository.deleteAll();
        salaRepository.deleteAll();
        lab = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
        auditorio = salaRepository.save(new Sala("Auditório", TipoSala.AUDITORIO, 200));
    }

    @AfterEach
    void restaurar() {
        RestAssured.reset();
    }

    /** Converte "HH:mm" em data/hora no dia seguinte, formatada em ISO-8601. */
    private static String amanha(String hhmm) {
        return ISO.format(LocalDateTime.parse(AMANHA.toLocalDate() + "T" + hhmm));
    }

    private static String json(Long salaId, String solicitante, String inicio, String fim) {
        return """
                {"salaId": %s, "solicitante": %s, "inicio": %s, "fim": %s}
                """.formatted(salaId, texto(solicitante), texto(inicio), texto(fim));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static ValidatableResponse postar(Long salaId, String solicitante, String inicio, String fim) {
        return given().contentType(ContentType.JSON).body(json(salaId, solicitante, inicio, fim))
                .when().post()
                .then();
    }

    /** Cria uma reserva no Lab 101 para amanhã no horário informado e devolve o id. */
    private long reservarLab(String inicio, String fim) {
        return postar(lab.getId(), "Ana", amanha(inicio), amanha(fim))
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Nested
    @DisplayName("POST /api/reservas — sucesso")
    class CriarComSucesso {

        @Test
        @DisplayName("201: cria reserva ATIVA e devolve Location e corpo")
        void criaReserva() {
            postar(lab.getId(), "  Ana Souza ", amanha("10:00"), amanha("12:00"))
                .statusCode(201)
                .header("Location", matchesPattern(".*/api/reservas/\\d+$"))
                .body("salaId", equalTo(lab.getId().intValue()))
                .body("salaNome", equalTo("Lab 101"))
                .body("solicitante", equalTo("Ana Souza"))
                .body("inicio", equalTo(amanha("10:00")))
                .body("fim", equalTo(amanha("12:00")))
                .body("status", equalTo("ATIVA"));
        }

        @Test
        @DisplayName("201: duração de exatamente 8 horas (valor-limite)")
        void duracaoMaximaExata() {
            postar(lab.getId(), "Ana", amanha("08:00"), amanha("16:00")).statusCode(201);
        }

        @Test
        @DisplayName("201: reservas adjacentes (antes e depois) não conflitam")
        void reservasAdjacentes() {
            reservarLab("10:00", "12:00");

            postar(lab.getId(), "Bia", amanha("08:00"), amanha("10:00")).statusCode(201);
            postar(lab.getId(), "Caio", amanha("12:00"), amanha("13:00")).statusCode(201);
        }

        @Test
        @DisplayName("201: mesmo horário em outra sala não conflita")
        void outraSalaMesmoHorario() {
            reservarLab("10:00", "12:00");

            postar(auditorio.getId(), "Bia", amanha("10:00"), amanha("12:00")).statusCode(201);
        }
    }

    @Nested
    @DisplayName("POST /api/reservas — conflito (409)")
    class Conflito {

        @ParameterizedTest(name = "[{0} – {1}) conflita com [10:00 – 12:00)")
        @CsvSource({
            "10:00, 12:00",  // idêntico
            "09:00, 10:01",  // invade o início
            "11:59, 13:00",  // invade o fim
            "10:30, 11:30",  // contido
            "09:00, 13:00"   // contém
        })
        void rejeitaSobreposicao(String inicio, String fim) {
            reservarLab("10:00", "12:00");

            postar(lab.getId(), "Bia", amanha(inicio), amanha(fim))
                .statusCode(409)
                .body("status", equalTo(409))
                .body("mensagem", equalTo("A sala já está reservada em parte do período informado."));
        }
    }

    @Nested
    @DisplayName("POST /api/reservas — validações (400/404)")
    class Validacoes {

        @Test
        @DisplayName("400: campos obrigatórios ausentes")
        void camposObrigatorios() {
            postar(null, "", null, null)
                .statusCode(400)
                .body("mensagem", equalTo("Dados inválidos."))
                .body("detalhes", containsInAnyOrder(
                        "fim: A data de fim é obrigatória.",
                        "inicio: A data de início é obrigatória.",
                        "salaId: A sala é obrigatória.",
                        "solicitante: O solicitante é obrigatório."));
        }

        @Test
        @DisplayName("400: fim anterior ao início")
        void fimAntesDoInicio() {
            postar(lab.getId(), "Ana", amanha("12:00"), amanha("10:00"))
                .statusCode(400)
                .body("mensagem", equalTo("A data de fim deve ser posterior à data de início."));
        }

        @Test
        @DisplayName("400: início no passado")
        void inicioNoPassado() {
            String ontem = ISO.format(AMANHA.minusDays(2).withHour(10));
            String ontemFim = ISO.format(AMANHA.minusDays(2).withHour(11));

            postar(lab.getId(), "Ana", ontem, ontemFim)
                .statusCode(400)
                .body("mensagem", equalTo("Não é possível reservar com início no passado."));
        }

        @Test
        @DisplayName("400: duração acima de 8 horas (8h01)")
        void duracaoExcessiva() {
            postar(lab.getId(), "Ana", amanha("08:00"), amanha("16:01"))
                .statusCode(400)
                .body("mensagem", equalTo("A reserva não pode ultrapassar 8 horas."));
        }

        @Test
        @DisplayName("400: data em formato inválido")
        void dataInvalida() {
            postar(lab.getId(), "Ana", "31/12/2030 10:00", amanha("12:00"))
                .statusCode(400)
                .body("mensagem", equalTo("Corpo da requisição inválido ou mal formatado."));
        }

        @Test
        @DisplayName("400: sala inativa não aceita reservas")
        void salaInativa() {
            lab.desativar();
            salaRepository.save(lab);

            postar(lab.getId(), "Ana", amanha("10:00"), amanha("11:00"))
                .statusCode(400)
                .body("mensagem", equalTo("A sala está inativa e não aceita reservas."));
        }

        @Test
        @DisplayName("404: sala inexistente")
        void salaInexistente() {
            postar(999L, "Ana", amanha("10:00"), amanha("11:00"))
                .statusCode(404)
                .body("mensagem", equalTo("Sala não encontrada: 999"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/reservas/{id}/cancelar")
    class Cancelar {

        @Test
        @DisplayName("200: cancela a reserva e libera o horário para nova reserva")
        void cancelaELiberaHorario() {
            long id = reservarLab("10:00", "12:00");

            given().when().patch("/{id}/cancelar", id)
                .then().statusCode(200).body("status", equalTo("CANCELADA"));

            postar(lab.getId(), "Bia", amanha("10:00"), amanha("12:00")).statusCode(201);
        }

        @Test
        @DisplayName("400: não permite cancelar duas vezes")
        void cancelaDuasVezes() {
            long id = reservarLab("10:00", "12:00");
            given().when().patch("/{id}/cancelar", id).then().statusCode(200);

            given().when().patch("/{id}/cancelar", id)
                .then()
                .statusCode(400)
                .body("mensagem", equalTo("Somente reservas ativas podem ser canceladas."));
        }

        @Test
        @DisplayName("404: reserva inexistente")
        void reservaInexistente() {
            given().when().patch("/{id}/cancelar", 999)
                .then().statusCode(404).body("mensagem", equalTo("Reserva não encontrada: 999"));
        }
    }

    @Nested
    @DisplayName("GET /api/reservas")
    class Consultar {

        @Test
        @DisplayName("200: lista as reservas, da mais recente para a mais antiga")
        void lista() {
            reservarLab("08:00", "09:00");
            reservarLab("14:00", "15:00");

            given().when().get()
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].inicio", equalTo(amanha("14:00")))
                .body("[1].inicio", equalTo(amanha("08:00")));
        }

        @Test
        @DisplayName("200 / 404: busca por id")
        void buscaPorId() {
            long id = reservarLab("08:00", "09:00");

            given().when().get("/{id}", id).then().statusCode(200).body("solicitante", equalTo("Ana"));
            given().when().get("/{id}", 999).then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("Encerramento automático (RF07)")
    class EncerramentoAutomatico {

        @Test
        @DisplayName("reserva vencida passa a ENCERRADA e a sala fica livre; reserva futura continua ATIVA")
        void encerraReservaVencidaELiberaSala() {
            LocalDateTime agora = LocalDateTime.now();
            // Reservas inseridas diretamente no banco, pois a API não aceita datas passadas.
            Reserva vencida = reservaRepository.save(
                    new Reserva(lab, "Ana", agora.minusHours(3), agora.minusHours(1)));
            long futura = reservarLab("10:00", "11:00");

            int encerradas = encerramentoJob.executar();

            assertEquals(1, encerradas);
            given().when().get("/{id}", vencida.getId()).then().body("status", equalTo("ENCERRADA"));
            given().when().get("/{id}", futura).then().body("status", equalTo("ATIVA"));
            given().basePath("/api/salas").when().get("/{id}", lab.getId())
                .then().body("ocupada", equalTo(false));
        }
    }
}
