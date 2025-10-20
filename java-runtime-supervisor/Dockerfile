FROM eclipse-temurin:17-jre

# Install Docker CLI for managing worker containers
RUN apt-get update && \
    apt-get install -y docker.io && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY java-runtime-supervisor/target/*.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]

