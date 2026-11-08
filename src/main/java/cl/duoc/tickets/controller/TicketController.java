package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketResponse;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.service.TicketService;
import cl.duoc.tickets.service.TicketService.Archivo;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    /** Genera el ticket (PDF en EFS) y, si la subida automatica esta activa, lo sube a S3. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse generar(@Valid @RequestBody TicketRequest req) {
        return service.generar(req);
    }

    /** Historial de tickets generados, con filtros opcionales. */
    @GetMapping
    public List<TicketResponse> historial(@RequestParam(required = false) String evento,
                                          @RequestParam(required = false) String usuario,
                                          @RequestParam(required = false) EstadoTicket estado) {
        return service.historial(evento, usuario, estado);
    }

    @GetMapping("/{id}")
    public TicketResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Sube (o vuelve a subir) un ticket a S3. */
    @PostMapping("/{id}/subir-s3")
    public TicketResponse subir(@PathVariable Long id) {
        return service.subir(id);
    }

    /** Sube a S3 todos los tickets que aun estan solo en EFS. */
    @PostMapping("/subir-s3/pendientes")
    public List<TicketResponse> subirPendientes() {
        return service.subirPendientes();
    }

    /** Descarga el PDF de un ticket desde S3. */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return archivo(service.descargar(id), MediaType.APPLICATION_PDF);
    }

    /** Descarga un ZIP con los tickets de un evento y/o usuario. */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarZip(@RequestParam(required = false) String evento,
                                               @RequestParam(required = false) String usuario) {
        return archivo(service.descargarZip(evento, usuario), MediaType.parseMediaType("application/zip"));
    }

    /** Modifica los detalles del ticket; se regenera el PDF y se actualiza en S3. */
    @PutMapping("/{id}")
    public TicketResponse modificar(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest req) {
        return service.modificar(id, req);
    }

    /** Elimina el ticket de S3, de EFS y de la base de datos. */
    @DeleteMapping("/{id}")
    public Map<String, Object> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return Map.of("mensaje", "Ticket eliminado de S3, EFS y base de datos", "id", id);
    }

    private ResponseEntity<byte[]> archivo(Archivo archivo, MediaType tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .contentType(tipo)
                .body(archivo.contenido());
    }
}
