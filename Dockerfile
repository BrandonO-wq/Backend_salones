# ── Build stage ──
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src/ src/

RUN ./gradlew build --no-daemon -x test

# ── Runtime stage ──
FROM eclipse-temurin:21-jre-jammy

ENV TZ=America/Lima

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]