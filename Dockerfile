# Этап 1: Сборка
FROM maven:3.9.3-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

EXPOSE 8080

COPY --from=builder /app/target/game-recommendation-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xmx512m", "-jar", "app.jar"]