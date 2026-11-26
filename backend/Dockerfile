# Build multi-stage: la etapa de compilación usa JDK completo y no viaja a la imagen final.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# Se copian primero los archivos de build para aprovechar la caché de capas de Docker:
# si solo cambia el código, Gradle reutiliza las dependencias ya descargadas.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null || true
COPY src src
# `-x test` es intencionado: las pruebas se ejecutan antes en scripts/test-all.ps1
# (con Testcontainers) y dentro de la imagen no hay Docker disponible.
RUN ./gradlew --no-daemon bootJar -x test

# Etapa de ejecución: solo JRE, sin herramientas de build ni código fuente.
FROM eclipse-temurin:21-jre
WORKDIR /app
# `curl` existe únicamente para el healthcheck de Compose que consulta /actuator/health.
# El usuario `spring` (uid 1001) evita ejecutar la API como root dentro del contenedor.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* && useradd -r -u 1001 spring
COPY --from=build /src/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
# MaxRAMPercentage=75 deja memoria para PostgreSQL cuando ambos comparten equipo,
# como ocurre en el desarrollo local y en el stack aislado de pruebas.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
