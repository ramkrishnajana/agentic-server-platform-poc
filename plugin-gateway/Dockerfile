FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw clean package -DskipTests -pl plugin-gateway -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/plugin-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

