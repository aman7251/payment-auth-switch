# --- Stage 1: build the jar with Maven ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Pre-download dependencies (cached layer; speeds up rebuilds)
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -e -B clean package -DskipTests

# --- Stage 2: small runtime image with just the JRE + jar ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# REST/HTTP on 8080 (or $PORT), ISO 8583 TCP on 10000
EXPOSE 8080 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
