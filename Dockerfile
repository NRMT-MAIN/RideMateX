# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first for better layer caching
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Copy source
COPY src ./src

# Build the application
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the fat jar from the build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot HTTP port + gRPC port
EXPOSE 8080 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
