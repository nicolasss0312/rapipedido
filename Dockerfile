# --- Compilación (no hace falta tener Java ni Maven instalados) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Imagen de ejecución ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/rapipedido.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
