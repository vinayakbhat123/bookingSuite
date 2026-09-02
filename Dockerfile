# ------------------------------------------------------------------------------
# Stage 1: Build & Package Spring Boot Application
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and dependencies specification
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Pre-fetch dependencies in a separate layer for build cache optimization
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production jar (skipping unit tests for fast Docker builds)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Extract jar layers for layered Docker image optimization
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination target/extracted || \
    cp target/*.jar target/app.jar

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create a dedicated non-root user and group for security hardening
RUN addgroup -S spring && adduser -S spring -G spring

# Copy application jar from builder
COPY --from=builder --chown=spring:spring /build/target/*.jar /app/app.jar

# Expose backend API port
EXPOSE 8080

# Environment variables with sensible defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Drop privileges to non-root user
USER spring:spring

# Execute application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
