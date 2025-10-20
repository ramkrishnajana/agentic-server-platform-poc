FROM eclipse-temurin:17-jre
WORKDIR /app
COPY java-plugin-add/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

