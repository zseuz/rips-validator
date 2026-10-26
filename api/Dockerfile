# Se construye desde la raíz del repositorio (docker compose usa context: .) para incluir catalogos/.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app/api
COPY api/.mvn .mvn
COPY api/mvnw api/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY catalogos /app/catalogos
COPY api/src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/api/target/rips-validator-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
