package br.edu.ifrs.salaprecisa.api;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;

/**
 * Representação de uma sala devolvida pela API.
 */
public record SalaResponse(Long id, String nome, TipoSala tipo, int capacidade, boolean ativa, boolean ocupada) {

    public static SalaResponse de(Sala sala, boolean ocupada) {
        return new SalaResponse(sala.getId(), sala.getNome(), sala.getTipo(),
                sala.getCapacidade(), sala.isAtiva(), ocupada);
    }
}
