package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.Evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventoResponse(
        Long id,
        String codigo,
        String nombre,
        String lugar,
        LocalDateTime fecha,
        Integer capacidad,
        BigDecimal precio) {

    public static EventoResponse de(Evento e) {
        return new EventoResponse(e.getId(), e.getCodigo(), e.getNombre(), e.getLugar(),
                e.getFecha(), e.getCapacidad(), e.getPrecio());
    }
}
