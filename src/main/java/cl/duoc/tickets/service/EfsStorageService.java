package cl.duoc.tickets.service;

import cl.duoc.tickets.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Almacenamiento temporal en EFS. En la EC2 el EFS se monta en /mnt/efs
 * y se comparte con el contenedor, por lo que aqui solo se escribe en una carpeta.
 * La estructura es la misma que en S3: evento/usuario/ticketNNN.pdf
 */
@Service
public class EfsStorageService {

    private static final Logger log = LoggerFactory.getLogger(EfsStorageService.class);

    private final Path base;

    public EfsStorageService(AppProperties props) {
        this.base = Paths.get(props.efs().ruta()).toAbsolutePath().normalize();
    }

    public Path guardar(String rutaRelativa, byte[] contenido) {
        Path destino = resolver(rutaRelativa);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
            log.info("Ticket guardado en EFS: {}", destino);
            return destino;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en EFS: " + destino, e);
        }
    }

    public byte[] leer(String rutaAbsoluta) {
        try {
            return Files.readAllBytes(Paths.get(rutaAbsoluta));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer desde EFS: " + rutaAbsoluta, e);
        }
    }

    public boolean existe(String rutaAbsoluta) {
        return rutaAbsoluta != null && Files.exists(Paths.get(rutaAbsoluta));
    }

    public void eliminar(String rutaAbsoluta) {
        if (rutaAbsoluta == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(rutaAbsoluta));
            log.info("Ticket eliminado de EFS: {}", rutaAbsoluta);
        } catch (IOException e) {
            log.warn("No se pudo eliminar de EFS: {}", rutaAbsoluta, e);
        }
    }

    private Path resolver(String rutaRelativa) {
        Path destino = base.resolve(rutaRelativa).normalize();
        if (!destino.startsWith(base)) {
            throw new IllegalArgumentException("Ruta no permitida: " + rutaRelativa);
        }
        return destino;
    }
}
