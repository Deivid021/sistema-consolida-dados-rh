# Imagens multiarquitetura (amd64 e arm64), compatíveis com a EC2 t4g

# 1. Build do front-end (React + Vite)
FROM node:22-alpine AS front
WORKDIR /front
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Build do back-end, com o front-end dentro do jar (classpath:/static)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
COPY --from=front /front/dist ./src/main/resources/static
RUN mvn -q package -DskipTests

# 3. Imagem final: um contêiner serve o site e a API no mesmo endereço
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app
COPY --from=build /app/target/rh-gravatai-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "app.jar"]
