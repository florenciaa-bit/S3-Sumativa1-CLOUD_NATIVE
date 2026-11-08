package cl.duoc.tickets.service;

import cl.duoc.tickets.config.AppProperties;
import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketResponse;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.exception.ConflictoException;
import cl.duoc.tickets.exception.NoEncontradoException;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Evento;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Flujo de un ticket:
 * 1. Se genera el PDF y se guarda en EFS (almacenamiento temporal).
 * 2. Se sube a S3 con la ruta evento/usuario/ticketNNN.pdf.
 * 3. Se puede modificar (se regenera el PDF y se actualiza en S3), descargar o eliminar.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository tickets;
    private final EventoService eventoService;
    private final PdfTicketGenerator pdf;
    private final EfsStorageService efs;
    private final S3StorageService s3;
    private final boolean subidaAutomatica;

    public TicketService(TicketRepository tickets, EventoService eventoService, PdfTicketGenerator pdf,
                         EfsStorageService efs, S3StorageService s3, AppProperties props) {
        this.tickets = tickets;
        this.eventoService = eventoService;
        this.pdf = pdf;
        this.efs = efs;
        this.s3 = s3;
        this.subidaAutomatica = props.s3().subidaAutomatica();
    }

    @Transactional
    public TicketResponse generar(TicketRequest req) {
        Evento evento = eventoService.buscarEntidad(req.eventoCodigo());
        if (tickets.countByEventoId(evento.getId()) >= evento.getCapacidad()) {
            throw new ConflictoException("El evento " + evento.getCodigo() + " no tiene cupos disponibles");
        }

        Ticket t = new Ticket();
        t.setEvento(evento);
        t.setUsuario(req.usuario().trim());
        t.setAsiento(req.asiento());
        t.setPrecio(evento.getPrecio());
        t.setEstado(req.estado() == null ? EstadoTicket.RESERVADO : req.estado());
        t = tickets.save(t);
        t.setCodigo(String.format("ticket%03d", t.getId()));

        guardarEnEfs(t);
        if (subidaAutomatica) {
            subirAS3(t);
        }
        return TicketResponse.de(t);
    }

    @Transactional
    public TicketResponse subir(Long id) {
        Ticket t = buscar(id);
        subirAS3(t);
        return TicketResponse.de(t);
    }

    /** Sube a S3 todos los tickets que todavia no estan alla. */
    @Transactional
    public List<TicketResponse> subirPendientes() {
        List<Ticket> pendientes = tickets.findByS3KeyIsNull();
        pendientes.forEach(this::subirAS3);
        return pendientes.stream().map(TicketResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> historial(String evento, String usuario, EstadoTicket estado) {
        return tickets.buscar(vacioANull(evento), vacioANull(usuario), estado).stream()
                .map(TicketResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse obtener(Long id) {
        return TicketResponse.de(buscar(id));
    }

    @Transactional
    public TicketResponse modificar(Long id, TicketUpdateRequest req) {
        Ticket t = buscar(id);
        String rutaEfsAnterior = t.getRutaEfs();
        String keyAnterior = t.getS3Key();

        if (req.usuario() != null && !req.usuario().isBlank()) {
            t.setUsuario(req.usuario().trim());
        }
        if (req.asiento() != null) {
            t.setAsiento(req.asiento());
        }
        if (req.estado() != null) {
            t.setEstado(req.estado());
        }
        if (req.precio() != null) {
            t.setPrecio(req.precio());
        }

        // Se regenera el PDF con los datos nuevos y se reemplaza en EFS y S3
        guardarEnEfs(t);
        if (rutaEfsAnterior != null && !rutaEfsAnterior.equals(t.getRutaEfs())) {
            efs.eliminar(rutaEfsAnterior);
        }
        subirAS3(t);
        if (keyAnterior != null && !keyAnterior.equals(t.getS3Key())) {
            s3.eliminar(keyAnterior);
        }
        return TicketResponse.de(t);
    }

    @Transactional
    public void eliminar(Long id) {
        Ticket t = buscar(id);
        if (t.getS3Key() != null) {
            s3.eliminar(t.getS3Key());
        }
        efs.eliminar(t.getRutaEfs());
        tickets.delete(t);
    }

    /** Descarga el PDF de un ticket desde S3. */
    @Transactional(readOnly = true)
    public Archivo descargar(Long id) {
        Ticket t = buscar(id);
        if (t.getS3Key() == null) {
            throw new ConflictoException("El ticket " + t.getCodigo() + " aun no se ha subido a S3");
        }
        return new Archivo(t.getCodigo() + ".pdf", s3.descargar(t.getS3Key()));
    }

    /** Descarga un ZIP con los tickets de un evento y/o usuario, leidos desde S3. */
    @Transactional(readOnly = true)
    public Archivo descargarZip(String evento, String usuario) {
        String ev = vacioANull(evento);
        String us = vacioANull(usuario);
        if (ev == null && us == null) {
            throw new IllegalArgumentException("Debe indicar evento y/o usuario");
        }
        List<Ticket> lista = tickets.buscar(ev, us, null).stream()
                .filter(t -> t.getS3Key() != null)
                .toList();
        if (lista.isEmpty()) {
            throw new NoEncontradoException("No hay tickets en S3 para los filtros indicados");
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Ticket t : lista) {
                zip.putNextEntry(new ZipEntry(t.getS3Key()));
                zip.write(s3.descargar(t.getS3Key()));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el ZIP", e);
        }
        String nombre = "tickets" + (ev != null ? "_" + ev : "") + (us != null ? "_" + limpiar(us) : "") + ".zip";
        return new Archivo(nombre, salida.toByteArray());
    }

    // ---------------------------------------------------------------------

    /** Estructura comun para EFS y S3: evento/usuario/ticketNNN.pdf */
    static String rutaRelativa(Ticket t) {
        return t.getEvento().getCodigo() + "/" + limpiar(t.getUsuario()) + "/" + t.getCodigo() + ".pdf";
    }

    private void guardarEnEfs(Ticket t) {
        Path ruta = efs.guardar(rutaRelativa(t), pdf.generar(t));
        t.setRutaEfs(ruta.toString());
    }

    private void subirAS3(Ticket t) {
        byte[] contenido = efs.existe(t.getRutaEfs()) ? efs.leer(t.getRutaEfs()) : pdf.generar(t);
        String key = rutaRelativa(t);
        s3.subir(key, contenido);
        t.setS3Key(key);
        log.info("Ticket {} disponible en S3 como {}", t.getCodigo(), key);
    }

    private Ticket buscar(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el ticket con id " + id));
    }

    private static String limpiar(String texto) {
        return texto.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public record Archivo(String nombre, byte[] contenido) {
    }
}
