FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
COPY pom.xml .
COPY src ./src
RUN ./mvnw -B -ntp -Pproduction -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app
COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
