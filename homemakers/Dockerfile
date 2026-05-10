# ── Stage 1: Build ────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix mvnw permissions
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Run ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN mkdir -p uploads

COPY --from=build /app/target/homemakers-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]