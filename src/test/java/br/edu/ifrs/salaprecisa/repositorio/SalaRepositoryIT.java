package br.edu.ifrs.salaprecisa.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Teste de componente da camada de persistência de salas, executado contra o H2 real.
 */
@DataJpaTest
@DisplayName("SalaRepository (componente de persistência)")
class SalaRepositoryIT {

    @Autowired
    private SalaRepository salaRepository;

    @Test
    @DisplayName("existsByNomeIgnoreCase não diferencia maiúsculas de minúsculas")
    void nomeIgnorandoCaixa() {
        salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));

        assertThat(salaRepository.existsByNomeIgnoreCase("LAB 101")).isTrue();
        assertThat(salaRepository.existsByNomeIgnoreCase("lab 102")).isFalse();
    }

    @Test
    @DisplayName("existsByNomeIgnoreCaseAndIdNot desconsidera a própria sala")
    void nomeDeOutraSala() {
        Sala lab = salaRepository.save(new Sala("Lab 101", TipoSala.LABORATORIO, 30));
        Sala auditorio = salaRepository.save(new Sala("Auditório", TipoSala.AUDITORIO, 200));

        assertThat(salaRepository.existsByNomeIgnoreCaseAndIdNot("lab 101", lab.getId())).isFalse();
        assertThat(salaRepository.existsByNomeIgnoreCaseAndIdNot("lab 101", auditorio.getId())).isTrue();
    }

    @Test
    @DisplayName("listagens são ordenadas por nome e listarAtivas exclui salas inativas")
    void listagensOrdenadas() {
        Sala c = salaRepository.save(new Sala("C - Sala", TipoSala.OUTRO, 5));
        Sala a = salaRepository.save(new Sala("A - Sala", TipoSala.OUTRO, 5));
        Sala b = new Sala("B - Sala", TipoSala.OUTRO, 5);
        b.desativar();
        b = salaRepository.save(b);

        assertThat(salaRepository.findAllByOrderByNomeAsc()).containsExactly(a, b, c);
        assertThat(salaRepository.findByAtivaTrueOrderByNomeAsc()).containsExactly(a, c);
    }

    @Test
    @DisplayName("o banco impede duas salas com o mesmo nome (restrição UNIQUE)")
    void restricaoDeNomeUnico() {
        salaRepository.saveAndFlush(new Sala("Lab 101", TipoSala.LABORATORIO, 30));

        assertThatThrownBy(() -> salaRepository.saveAndFlush(new Sala("Lab 101", TipoSala.OUTRO, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
