package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.EstadisticaEvento;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Evento;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.repository.EventoRepository;
import cl.duoc.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Estadisticas de ventas (PAGADO) y reservas (RESERVADO) por evento. */
@Service
public class EstadisticaService {

    private final EventoRepository eventos;
    private final TicketRepository tickets;
    private final EventoService eventoService;

    public EstadisticaService(EventoRepository eventos, TicketRepository tickets, EventoService eventoService) {
        this.eventos = eventos;
        this.tickets = tickets;
        this.eventoService = eventoService;
    }

    @Transactional(readOnly = true)
    public List<EstadisticaEvento> porEvento() {
        return eventos.findAll().stream().map(this::calcular).toList();
    }

    @Transactional(readOnly = true)
    public EstadisticaEvento deEvento(String codigo) {
        return calcular(eventoService.buscarEntidad(codigo));
    }

    private EstadisticaEvento calcular(Evento e) {
        List<Ticket> lista = tickets.findByEventoCodigo(e.getCodigo());
        long ventas = lista.stream().filter(t -> t.getEstado() == EstadoTicket.PAGADO).count();
        long reservas = lista.stream().filter(t -> t.getEstado() == EstadoTicket.RESERVADO).count();
        BigDecimal recaudacion = sumar(lista, EstadoTicket.PAGADO);
        BigDecimal montoReservado = sumar(lista, EstadoTicket.RESERVADO);
        long enS3 = lista.stream().filter(t -> t.getS3Key() != null).count();
        double ocupacion = e.getCapacidad() == 0 ? 0
                : BigDecimal.valueOf(lista.size() * 100.0 / e.getCapacidad())
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        return new EstadisticaEvento(e.getCodigo(), e.getNombre(), e.getCapacidad(), lista.size(),
                ventas, reservas, recaudacion, montoReservado, ocupacion,
                Math.max(0, e.getCapacidad() - lista.size()), enS3);
    }

    private BigDecimal sumar(List<Ticket> lista, EstadoTicket estado) {
        return lista.stream()
                .filter(t -> t.getEstado() == estado)
                .map(Ticket::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
