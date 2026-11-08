# Guía de despliegue en AWS Academy

Región: **us-east-1 (N. Virginia)**, la única habilitada en el Learner Lab.

## 1. Bucket S3
1. S3 → Create bucket → nombre único, por ejemplo `tickets-mburgos-2026`.
2. Dejar "Block all public access" activado → Create bucket.

## 2. Security groups
EC2 → Security Groups → Create security group:

| Nombre | Regla de entrada | Origen |
|---|---|---|
| `tickets-sg` | SSH 22 | Anywhere-IPv4 (lo usa GitHub Actions) |
| `tickets-sg` | Custom TCP 8080 | Anywhere-IPv4 (lo usa Postman) |
| `efs-sg` | NFS 2049 | `tickets-sg` |

## 3. Instancia EC2
EC2 → Launch instance:
- Nombre `tickets-ec2`, AMI **Amazon Linux 2023**, tipo **t3.small**.
- Key pair **vockey**.
- Security group existente: `tickets-sg`.
- Storage: 16 GiB.
- Advanced details → IAM instance profile: **LabInstanceProfile**; Metadata response hop limit: **2**.

## 4. EFS
EFS → Create file system → Customize:
- Nombre `tickets-efs`, VPC por defecto.
- En Network, en cada zona reemplazar el security group por `efs-sg`.
- Anotar el File system ID (`fs-...`).

## 5. Preparar la EC2
Conectarse con EC2 Instance Connect y ejecutar (reemplazar `fs-XXXX`):

```bash
sudo dnf install -y docker amazon-efs-utils unzip
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

sudo mkdir -p /mnt/efs
sudo mount -t efs -o tls fs-XXXX:/ /mnt/efs
echo "fs-XXXX:/ /mnt/efs efs _netdev,tls 0 0" | sudo tee -a /etc/fstab
sudo mkdir -p /mnt/efs/tickets
df -h | grep efs
```

## 6. Wallet de Oracle y variables
1. Subir el zip de la wallet al bucket en la carpeta `config/` desde la consola de S3.
2. En la EC2:

```bash
cd /home/ec2-user
aws s3 cp s3://NOMBRE_BUCKET/config/Wallet_TICKETSDB.zip .
unzip -o Wallet_TICKETSDB.zip -d wallet
aws s3 rm s3://NOMBRE_BUCKET/config/Wallet_TICKETSDB.zip
rm Wallet_TICKETSDB.zip
nano tickets.env      # pegar el contenido de deploy/tickets.env.example con los valores reales
chmod 600 tickets.env
```

## 7. Secrets en GitHub
Repositorio → Settings → Secrets and variables → Actions → New repository secret:

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | access token de Docker Hub (Read & Write) |
| `EC2_HOST` | IP pública de la EC2 |
| `EC2_SSH_KEY` | contenido completo de `labsuser.pem` (AWS Details → Download PEM) |

## 8. Desplegar
Push a `main` → pestaña Actions → los tres jobs deben quedar en verde.
Probar `http://IP_PUBLICA:8080/actuator/health` y luego la colección de Postman.

## Cada vez que se reinicia el Learner Lab
- La IP pública de la EC2 cambia: actualizar el secret `EC2_HOST` y `baseUrl` en Postman.
- El EFS se vuelve a montar solo (está en `/etc/fstab`) y el contenedor se reinicia solo (`--restart unless-stopped`).
- Verificar que la base Oracle esté en estado Disponible.
