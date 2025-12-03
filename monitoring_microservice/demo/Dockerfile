# STAGE 1: Build (Compilare)
FROM maven:3.8.5-openjdk-17 AS builder

# Copiem sursele și pom.xml în container
COPY ./src /usr/src/app/src
COPY ./pom.xml /usr/src/app

# Setăm directorul de lucru
WORKDIR /usr/src/app

# Rulăm comanda de build (fără teste pentru viteză)
RUN mvn clean package -DskipTests

# STAGE 2: Run (Rulare efectivă)
FROM openjdk:17-jdk-slim

# Copiem doar JAR-ul rezultat din prima etapă
COPY --from=builder /usr/src/app/target/*.jar app.jar

# Comanda de start
ENTRYPOINT ["java", "-jar", "/app.jar"]