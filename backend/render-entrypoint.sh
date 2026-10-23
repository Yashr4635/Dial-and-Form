#!/bin/sh
set -eu

# Render provides DATABASE_URL as a PostgreSQL URI; Spring's driver expects a
# JDBC URI. Keep an explicit SPRING_DATASOURCE_URL override for Docker/local use.
if [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
    normalized_url=$(printf '%s' "$DATABASE_URL" | sed 's|^postgres://|postgresql://|')
    export SPRING_DATASOURCE_URL="jdbc:${normalized_url}"
fi

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
