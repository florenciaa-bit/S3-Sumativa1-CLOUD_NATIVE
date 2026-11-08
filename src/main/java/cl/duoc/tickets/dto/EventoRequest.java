package cl.duoc.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventoRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "solo letras, numeros, guion y guion bajo")
        String codigo,
        @NotBlank String nombre,
        String lugar,
        LocalDateTime fecha,
        @NotNull @Positive Integer capacidad,
        @NotNull @PositiveOrZero BigDecimal precio) {
}
