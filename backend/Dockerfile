FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd --system --create-home --shell /usr/sbin/nologin appuser

COPY --from=build /workspace/target/*.jar app.jar

RUN printf '%s\n' '#!/bin/sh' \
    'set -e' \
    'if [ -n "$GOOGLE_CREDENTIALS_JSON" ]; then' \
    '  printf "%s" "$GOOGLE_CREDENTIALS_JSON" > /tmp/google-credentials.json' \
    '  chmod 600 /tmp/google-credentials.json' \
    'fi' \
    'exec java -jar /app/app.jar' \
    > /app/start.sh \
    && chmod +x /app/start.sh \
    && chown appuser:appuser /app/start.sh

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
	CMD curl -fsS http://127.0.0.1:8081/admin/login >/dev/null || exit 1

ENTRYPOINT ["/app/start.sh"]