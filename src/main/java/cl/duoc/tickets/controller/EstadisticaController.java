package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.EstadisticaEvento;
import cl.duoc.tickets.service.EstadisticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticaController {

    private final EstadisticaService service;

    public EstadisticaController(EstadisticaService service) {
        this.service = service;
    }

    /** Ventas y reservas de todos los eventos. */
    @GetMapping("/eventos")
    public List<EstadisticaEvento> porEvento() {
        return service.porEvento();
    }

    /** Ventas y reservas de un evento. */
    @GetMapping("/eventos/{codigo}")
    public EstadisticaEvento deEvento(@PathVariable String codigo) {
        return service.deEvento(codigo);
    }
}
