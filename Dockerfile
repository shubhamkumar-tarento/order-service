# =====================================================================
# Stage 1 - build the jar. Keeps the image reproducible from a clean
# clone: no host-side `mvn package` required before `docker build`.
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Warm the dependency cache in its own layer so it stays valid when only
# application sources change. Best-effort: go-offline cannot always resolve
# every plugin, and the package step below fetches whatever is still missing.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B clean package -DskipTests

# =====================================================================
# Stage 2 - runtime. JRE only, non-root uid 1001 (matches the
# runAsUser in k8s/deployment.yaml).
# =====================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S -G spring -u 1001 spring

COPY --from=build /build/target/order-service.jar app.jar

USER spring

# Must match server.port in application.properties and the containerPort
# in k8s/deployment.yaml.
EXPOSE 8081

# `sh -c` so JAVA_OPTS from the environment is actually applied; `exec`
# keeps the JVM as PID 1 so it receives SIGTERM on pod shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
