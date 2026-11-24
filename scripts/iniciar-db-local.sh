#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_DB_DIR="$PWD/.local-db"
mkdir -p "$TASK_DB_DIR"
if [ ! -d "$TASK_DB_DIR/data/mysql" ]; then
  mariadb-install-db --datadir="$TASK_DB_DIR/data" --auth-root-authentication-method=socket --skip-test-db > "$TASK_DB_DIR/inicializacion.log" 2>&1
fi
if ! mariadb-admin --socket="$TASK_DB_DIR/server.sock" ping >/dev/null 2>&1; then
  nohup mariadbd --datadir="$TASK_DB_DIR/data" --socket="$TASK_DB_DIR/server.sock" --pid-file="$TASK_DB_DIR/server.pid" --port=3308 --bind-address=127.0.0.1 --log-error="$TASK_DB_DIR/server.log" >/dev/null 2>&1 &
  for task_attempt in {1..50}; do
    if mariadb-admin --socket="$TASK_DB_DIR/server.sock" ping >/dev/null 2>&1; then break; fi
    sleep 0.2
  done
fi
mariadb-admin --socket="$TASK_DB_DIR/server.sock" ping
