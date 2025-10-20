FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw clean package -DskipTests -pl java-runtime-supervisor -am

FROM eclipse-temurin:17-jre-alpine

# Install Docker CLI for managing worker containers
RUN apk add --no-cache docker-cli

WORKDIR /app
COPY --from=builder /workspace/java-runtime-supervisor/target/*.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]

