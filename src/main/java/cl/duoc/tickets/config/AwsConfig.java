package cl.duoc.tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    /**
     * Cliente S3 con la cadena de credenciales por defecto:
     * variables de entorno, archivo ~/.aws/credentials o el rol de la instancia EC2
     * (LabInstanceProfile en AWS Academy). Asi no hay claves escritas en el codigo.
     */
    @Bean
    public S3Client s3Client(AppProperties props) {
        return S3Client.builder()
                .region(Region.of(props.s3().region()))
                .build();
    }
}
