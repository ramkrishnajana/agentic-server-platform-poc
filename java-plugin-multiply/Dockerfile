FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw clean package -DskipTests -pl java-plugin-multiply -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/java-plugin-multiply/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

