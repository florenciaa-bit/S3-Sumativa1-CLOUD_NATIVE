package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String codigo,
        String eventoCodigo,
        String eventoNombre,
        String usuario,
        String asiento,
        BigDecimal precio,
        EstadoTicket estado,
        String rutaEfs,
        String s3Key,
        boolean subidoS3,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public static TicketResponse de(Ticket t) {
        return new TicketResponse(t.getId(), t.getCodigo(), t.getEvento().getCodigo(),
                t.getEvento().getNombre(), t.getUsuario(), t.getAsiento(), t.getPrecio(),
                t.getEstado(), t.getRutaEfs(), t.getS3Key(), t.getS3Key() != null,
                t.getFechaCreacion(), t.getFechaActualizacion());
    }
}
