package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.EventoRequest;
import cl.duoc.tickets.dto.EventoResponse;
import cl.duoc.tickets.exception.ConflictoException;
import cl.duoc.tickets.exception.NoEncontradoException;
import cl.duoc.tickets.model.Evento;
import cl.duoc.tickets.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventos;

    public EventoService(EventoRepository eventos) {
        this.eventos = eventos;
    }

    @Transactional
    public EventoResponse crear(EventoRequest req) {
        if (eventos.existsByCodigo(req.codigo())) {
            throw new ConflictoException("Ya existe un evento con codigo " + req.codigo());
        }
        Evento e = new Evento();
        e.setCodigo(req.codigo());
        e.setNombre(req.nombre());
        e.setLugar(req.lugar());
        e.setFecha(req.fecha());
        e.setCapacidad(req.capacidad());
        e.setPrecio(req.precio());
        return EventoResponse.de(eventos.save(e));
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar() {
        return eventos.findAll().stream().map(EventoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(String codigo) {
        return EventoResponse.de(buscarEntidad(codigo));
    }

    Evento buscarEntidad(String codigo) {
        return eventos.findByCodigo(codigo)
                .orElseThrow(() -> new NoEncontradoException("No existe el evento " + codigo));
    }
}
