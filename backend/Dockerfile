# ─── Stage 1: Build ───
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
LABEL org.opencontainers.image.source="https://github.com/Sreehc/ByteCoach"
RUN addgroup -S bytecoach && adduser -S bytecoach -G bytecoach
COPY --from=build /app/target/*.jar app.jar
RUN chown -R bytecoach:bytecoach /app
USER bytecoach
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
