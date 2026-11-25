#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_CONFIG="${1:-config.demo.properties}"
TASK_JAR="target/barberia-v3-3.0.1.jar"
if [ ! -f "$TASK_CONFIG" ]; then
  echo "Falta el archivo privado de configuración. Consulte README.md." >&2
  exit 1
fi
if [ ! -f "$TASK_JAR" ]; then mvn package -DskipTests; fi
exec java -Duser.timezone=America/Santo_Domingo "-Dbarberia.config=$TASK_CONFIG" -jar "$TASK_JAR"
