package br.edu.ifrs.salaprecisa.api;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.servico.ReservaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de reservas.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaApiController {

    private final ReservaService reservaService;

    public ReservaApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<ReservaResponse> listar() {
        return reservaService.listar().stream()
                .map(ReservaResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable Long id) {
        return ReservaResponse.de(reservaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody ReservaRequest request) {
        Reserva reserva = reservaService.criar(request.salaId(), request.solicitante(),
                request.inicio(), request.fim());
        return ResponseEntity.created(URI.create("/api/reservas/" + reserva.getId()))
                .body(ReservaResponse.de(reserva));
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return ReservaResponse.de(reservaService.cancelar(id));
    }
}
