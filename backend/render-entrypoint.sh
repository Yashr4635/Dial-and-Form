#!/bin/sh
set -eu

# The application parses Render's DATABASE_URL before Spring starts, keeping
# database credentials separate from the JDBC host portion. Explicit
# SPRING_DATASOURCE_* overrides remain available for local/container use.

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
