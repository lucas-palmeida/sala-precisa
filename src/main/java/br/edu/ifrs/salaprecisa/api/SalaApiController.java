package br.edu.ifrs.salaprecisa.api;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.servico.ReservaService;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de salas.
 */
@RestController
@RequestMapping("/api/salas")
public class SalaApiController {

    private final SalaService salaService;
    private final ReservaService reservaService;

    public SalaApiController(SalaService salaService, ReservaService reservaService) {
        this.salaService = salaService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<SalaResponse> listar() {
        Set<Long> ocupadas = salaService.idsSalasOcupadasAgora();
        return salaService.listar().stream()
                .map(sala -> SalaResponse.de(sala, ocupadas.contains(sala.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public SalaResponse buscar(@PathVariable Long id) {
        return paraResposta(salaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<SalaResponse> cadastrar(@Valid @RequestBody SalaRequest request) {
        Sala sala = salaService.cadastrar(request.nome(), request.tipo(), request.capacidade());
        return ResponseEntity.created(URI.create("/api/salas/" + sala.getId()))
                .body(SalaResponse.de(sala, false));
    }

    @PutMapping("/{id}")
    public SalaResponse atualizar(@PathVariable Long id, @Valid @RequestBody SalaRequest request) {
        return paraResposta(salaService.atualizar(id, request.nome(), request.tipo(), request.capacidade()));
    }

    @PatchMapping("/{id}/desativar")
    public SalaResponse desativar(@PathVariable Long id) {
        return paraResposta(salaService.desativar(id));
    }

    @PatchMapping("/{id}/ativar")
    public SalaResponse ativar(@PathVariable Long id) {
        return paraResposta(salaService.ativar(id));
    }

    @GetMapping("/{id}/reservas")
    public List<ReservaResponse> listarReservas(@PathVariable Long id) {
        return reservaService.listarPorSala(id).stream()
                .map(ReservaResponse::de)
                .toList();
    }

    private SalaResponse paraResposta(Sala sala) {
        boolean ocupada = salaService.idsSalasOcupadasAgora().contains(sala.getId());
        return SalaResponse.de(sala, ocupada);
    }
}
