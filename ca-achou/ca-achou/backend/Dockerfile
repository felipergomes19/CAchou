# Imagem do backend para rodar em qualquer nuvem que aceite Docker (Render, Railway, Fly.io, Cloud Run...).
# Build (a partir da pasta backend/):  docker build -t achou-api .

# ---- etapa 1: compila o .jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copia só o pom primeiro: o Docker reaproveita o cache das dependências enquanto o pom não mudar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- etapa 2: imagem final, só com o JRE ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Não roda como root dentro do container.
RUN useradd --system --uid 1001 achou
COPY --from=build /app/target/*.jar app.jar
USER achou
# Limita o heap à memória do container (planos gratuitos têm ~512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
