package cl.duoc.tickets.dto;

import java.math.BigDecimal;

public record EstadisticaEvento(
        String eventoCodigo,
        String eventoNombre,
        int capacidad,
        long totalTickets,
        long ventas,
        long reservas,
        BigDecimal recaudacion,
        BigDecimal montoReservado,
        double ocupacionPorcentaje,
        long disponibles,
        long ticketsEnS3) {
}
