package br.edu.ifrs.salaprecisa.repositorio;

import br.edu.ifrs.salaprecisa.dominio.Reserva;
import br.edu.ifrs.salaprecisa.dominio.StatusReserva;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso a dados das reservas.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findAllByOrderByInicioDesc();

    List<Reserva> findBySalaIdOrderByInicioAsc(Long salaId);

    /** Reservas com o status informado cujo fim é anterior ou igual ao instante informado. */
    List<Reserva> findByStatusAndFimLessThanEqual(StatusReserva status, LocalDateTime instante);

    /**
     * Verifica se existe reserva com o status informado, na mesma sala, cujo período
     * se sobrepõe a [inicio, fim). Intervalos adjacentes não são considerados conflito.
     */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.sala.id = :salaId
              and r.status = :status
              and r.inicio < :fim
              and r.fim > :inicio
            """)
    boolean existeReservaSobreposta(@Param("salaId") Long salaId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim,
                                    @Param("status") StatusReserva status);

    /** Identificadores das salas que possuem reserva com o status informado em andamento no instante. */
    @Query("""
            select distinct r.sala.id from Reserva r
            where r.status = :status
              and r.inicio <= :instante
              and r.fim > :instante
            """)
    List<Long> idsSalasOcupadas(@Param("instante") LocalDateTime instante,
                                @Param("status") StatusReserva status);
}
