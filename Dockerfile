# Etapa 1: compilar el proyecto con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen liviana solo con el JRE y el jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/tickets-service.jar app.jar

# /mnt/efs/tickets -> se monta el EFS de la EC2
# /app/wallet      -> se monta la wallet de Oracle (nunca va dentro de la imagen)
RUN mkdir -p /mnt/efs/tickets /app/wallet

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
