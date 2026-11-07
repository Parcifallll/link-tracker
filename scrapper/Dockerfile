FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/scrapper-0.0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
