package cl.duoc.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros editables de la aplicacion (ver application.yml).
 * Todos se pueden sobrescribir con variables de entorno.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Efs efs, S3 s3) {

    /** ruta: carpeta donde esta montado el EFS dentro del contenedor. */
    public record Efs(String ruta) {
    }

    /**
     * bucket: nombre del bucket S3.
     * region: region de AWS (en AWS Academy es us-east-1).
     * subidaAutomatica: si es true, cada ticket generado se sube a S3 de inmediato.
     */
    public record S3(String bucket, String region, boolean subidaAutomatica) {
    }
}
