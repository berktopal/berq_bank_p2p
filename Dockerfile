# syntax=docker/dockerfile:1

# 1) React uygulamasını derle
FROM node:24-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Spring Boot jar'ını SPA gömülü olarak derle (testler CI'da koşar)
FROM eclipse-temurin:25-jdk-alpine AS api
WORKDIR /app
COPY p2p-transfer/.mvn .mvn
COPY p2p-transfer/mvnw p2p-transfer/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY p2p-transfer/src src
COPY --from=web /web/dist src/main/resources/static
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# 3) Küçük, root olmayan çalışma imajı
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S berq && adduser -S berq -G berq
USER berq
WORKDIR /app
COPY --from=api /app/app.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness | grep -q UP || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
