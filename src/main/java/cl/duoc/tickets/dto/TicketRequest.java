package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import jakarta.validation.constraints.NotBlank;

/** Datos para generar un ticket. Si estado viene vacio queda RESERVADO. */
public record TicketRequest(
        @NotBlank String eventoCodigo,
        @NotBlank String usuario,
        String asiento,
        EstadoTicket estado) {
}
