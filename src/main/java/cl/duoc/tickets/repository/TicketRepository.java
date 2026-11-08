package cl.duoc.tickets.repository;

import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Historial con filtros opcionales: si un filtro viene null, no se aplica. */
    @Query("""
            select t from Ticket t
            where (:evento is null or t.evento.codigo = :evento)
              and (:usuario is null or t.usuario = :usuario)
              and (:estado is null or t.estado = :estado)
            order by t.fechaCreacion desc
            """)
    List<Ticket> buscar(@Param("evento") String evento,
                        @Param("usuario") String usuario,
                        @Param("estado") EstadoTicket estado);

    List<Ticket> findByEventoCodigo(String eventoCodigo);

    List<Ticket> findByS3KeyIsNull();

    long countByEventoId(Long eventoId);
}
