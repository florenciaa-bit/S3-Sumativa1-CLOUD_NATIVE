package cl.duoc.tickets.service;

import cl.duoc.tickets.config.AppProperties;
import cl.duoc.tickets.dto.ArchivoS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

/** Operaciones sobre el bucket S3 donde quedan los tickets definitivos. */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final String bucket;

    public S3StorageService(S3Client s3, AppProperties props) {
        this.s3 = s3;
        this.bucket = props.s3().bucket();
    }

    public void subir(String key, byte[] contenido) {
        validarBucket();
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(contenido));
        log.info("Ticket subido a s3://{}/{}", bucket, key);
    }

    public byte[] descargar(String key) {
        validarBucket();
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).asByteArray();
    }

    public void eliminar(String key) {
        validarBucket();
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        log.info("Ticket eliminado de s3://{}/{}", bucket, key);
    }

    /** Lista los archivos del bucket que empiezan con el prefijo (vacio = todos). */
    public List<ArchivoS3> listar(String prefijo) {
        validarBucket();
        ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);
        if (prefijo != null && !prefijo.isBlank()) {
            req.prefix(prefijo);
        }
        return s3.listObjectsV2Paginator(req.build()).contents().stream()
                .map(o -> new ArchivoS3(o.key(), o.size(), o.lastModified()))
                .toList();
    }

    private void validarBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("No esta configurado el bucket S3 (variable S3_BUCKET)");
        }
    }
}
