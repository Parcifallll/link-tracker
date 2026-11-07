# Build stage
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -pl scrapper -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/scrapper/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
