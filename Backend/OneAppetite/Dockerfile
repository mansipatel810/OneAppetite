# Multi-stage build: compile with Maven, run on a slim JRE.
# Render builds this image from Backend/OneAppetite (set as Root Directory).

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy build descriptors first so Docker can cache the dependency layer
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
RUN mvn -B -q dependency:go-offline

# Copy sources and package the fat jar
COPY src src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/OneAppetite-0.0.1-SNAPSHOT.jar app.jar

# Render injects PORT at runtime; Spring Boot reads it via server.port=${PORT:8081}
EXPOSE 8081

# JVM tuning for Render's 512 MB free tier:
#   -XX:MaxRAMPercentage=75.0   cap heap at 75% of container RAM (~380 MB)
#   -XX:+UseSerialGC            smallest-footprint GC; ideal for tiny containers
#   -Xss256k                    256 KB thread stack (default is 1 MB; Tomcat spawns ~50 threads)
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseSerialGC", \
            "-Xss256k", \
            "-jar", "/app/app.jar"]
