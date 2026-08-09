FROM eclipse-temurin:21-jdk-alpine@sha256:1ff763083f2993d57d0bf374ab10bb3e2cb873af6c13a04458ebbd3e0337dc76 AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY search-service/build.gradle search-service/build.gradle
RUN ./gradlew :search-service:dependencies --no-daemon

COPY search-service/src search-service/src
RUN ./gradlew :search-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c

RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app
COPY --from=builder --chown=app:app /workspace/search-service/build/libs/search-service.jar /app/search-service.jar

USER app
EXPOSE 8081 9091

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/search-service.jar"]
