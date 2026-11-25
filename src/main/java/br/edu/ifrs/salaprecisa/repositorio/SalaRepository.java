package br.edu.ifrs.salaprecisa.repositorio;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a dados das salas (Spring Data JPA gera a implementação).
 */
public interface SalaRepository extends JpaRepository<Sala, Long> {

    List<Sala> findAllByOrderByNomeAsc();

    List<Sala> findByAtivaTrueOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
