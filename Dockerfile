# ---------- etapa 1: compila o projeto ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# garante que o gradlew funcione no Linux mesmo se vier com quebra de linha do Windows
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# baixa as dependências antes do código (camada em cache: rebuilds ficam mais rápidos)
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test
RUN cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" app.jar

# ---------- etapa 2: imagem final, só com o JRE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/app.jar app.jar
USER app

EXPOSE 8080
# usa até 75% da memória do container para o heap da JVM
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
