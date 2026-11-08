package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Campos modificables de un ticket. Los que vengan null no se cambian. */
public record TicketUpdateRequest(
        String usuario,
        String asiento,
        EstadoTicket estado,
        @PositiveOrZero BigDecimal precio) {
}
