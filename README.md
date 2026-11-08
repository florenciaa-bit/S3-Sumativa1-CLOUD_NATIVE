# Sistema de Gestión de Reservas de Eventos con Tickets

Microservicio Spring Boot de la Sumativa 1 de Desarrollo Cloud Native (CDY2204), Duoc UC.

## Arquitectura

```
Postman ──HTTP:8080──> EC2 (AWS Academy) ── contenedor Docker tickets-service
                         │                      │
                         │                      ├── EFS montado en /mnt/efs/tickets   (almacenamiento temporal)
                         │                      ├── S3  evento/usuario/ticketNNN.pdf   (almacenamiento definitivo)
                         │                      └── Oracle Autonomous Database        (datos y estadísticas)
                         │
GitHub (push a main) ──> GitHub Actions: pruebas → imagen en Docker Hub → despliegue en EC2 por SSH
```

Flujo de un ticket: se genera el PDF y se guarda en EFS, se sube a S3 con la ruta `evento/usuario/ticketNNN.pdf`, y después se puede modificar (se regenera el PDF y se reemplaza en S3), descargar o eliminar.

## Endpoints

| Método | Ruta | Función |
|---|---|---|
| POST | `/api/eventos` | Crear evento |
| GET | `/api/eventos` | Listar eventos |
| POST | `/api/tickets` | **Generar ticket** (PDF en EFS y subida automática a S3) |
| POST | `/api/tickets/{id}/subir-s3` | **Subir ticket a S3** |
| POST | `/api/tickets/subir-s3/pendientes` | Subir a S3 todos los tickets pendientes |
| GET | `/api/tickets/{id}/descargar` | **Descargar** PDF de un ticket desde S3 |
| GET | `/api/tickets/descargar?evento=&usuario=` | **Descargar** ZIP por evento y/o usuario |
| PUT | `/api/tickets/{id}` | **Modificar detalles** del ticket (actualiza el archivo en S3) |
| DELETE | `/api/tickets/{id}` | **Eliminar ticket** (S3, EFS y base de datos) |
| GET | `/api/tickets?evento=&usuario=&estado=` | Historial de tickets con filtros |
| GET | `/api/estadisticas/eventos` | **Estadísticas de ventas y reservas** por evento |
| GET | `/api/estadisticas/eventos/{codigo}` | Estadísticas de un evento |
| GET | `/api/archivos/s3?prefijo=` | Archivos que hay en el bucket S3 |
| GET | `/actuator/health` | Estado de la aplicación y de la base de datos |

La colección de Postman está en `postman/tickets-service.postman_collection.json`.

## Configuración

Todas las variables están en `src/main/resources/application.yml`. En la EC2 se definen en `/home/ec2-user/tickets.env` (plantilla en `deploy/tickets.env.example`).

| Variable | Uso |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a Oracle Autonomous Database con wallet |
| `S3_BUCKET`, `AWS_REGION` | Bucket donde se guardan los tickets |
| `S3_SUBIDA_AUTOMATICA` | `true` para subir cada ticket al generarlo |
| `EFS_PATH` | Carpeta del EFS dentro del contenedor |

Las credenciales de AWS no van en el código: la EC2 usa el rol `LabInstanceProfile`.

## CI/CD

`.github/workflows/ci-cd.yml` se ejecuta con cada push a `main`:

1. Compila y ejecuta las pruebas (`mvn test`).
2. Construye la imagen Docker y la publica en Docker Hub (`latest` y el SHA del commit).
3. Entra por SSH a la EC2, reemplaza el contenedor y verifica `/actuator/health`.

Secrets requeridos en GitHub: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_SSH_KEY`.

## Ejecutar en local (opcional)

```bash
docker build -t tickets-service .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local -e S3_SUBIDA_AUTOMATICA=false tickets-service
```

El perfil `local` usa H2 y una carpeta local en lugar de Oracle y EFS.
