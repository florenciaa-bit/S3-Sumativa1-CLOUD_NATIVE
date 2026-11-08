package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.ArchivoS3;
import cl.duoc.tickets.service.S3StorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Muestra lo que hay realmente en el bucket, para comprobar la estructura de carpetas. */
@RestController
@RequestMapping("/api/archivos")
public class ArchivoS3Controller {

    private final S3StorageService s3;

    public ArchivoS3Controller(S3StorageService s3) {
        this.s3 = s3;
    }

    /** Ejemplos: /api/archivos/s3 , /api/archivos/s3?prefijo=evento123/ */
    @GetMapping("/s3")
    public List<ArchivoS3> listar(@RequestParam(required = false) String prefijo) {
        return s3.listar(prefijo);
    }
}
