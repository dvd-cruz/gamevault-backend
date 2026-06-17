# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache de dependencias: copia o pom primeiro
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Codigo e empacotamento
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# A porta efetiva vem da variavel PORT (Render injeta-a); 8080 e so fallback local.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
