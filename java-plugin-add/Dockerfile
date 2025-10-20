FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw clean package -DskipTests -pl java-plugin-add -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/java-plugin-add/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

