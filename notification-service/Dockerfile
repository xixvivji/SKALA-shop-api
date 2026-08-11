FROM eclipse-temurin:21-jdk-alpine@sha256:1ff763083f2993d57d0bf374ab10bb3e2cb873af6c13a04458ebbd3e0337dc76 AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY notification-service/build.gradle notification-service/build.gradle
RUN ./gradlew :notification-service:dependencies --no-daemon

COPY notification-service/src notification-service/src
RUN ./gradlew :notification-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c

RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app
COPY --from=builder --chown=app:app \
    /workspace/notification-service/build/libs/notification-service.jar \
    /app/notification-service.jar

USER app
EXPOSE 8082 9093

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/notification-service.jar"]
