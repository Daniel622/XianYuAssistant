#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${SERVER_PORT:-12400}"
LOG_DIR="$ROOT_DIR/logs"
DB_DIR="$ROOT_DIR/dbdata"
PID_FILE="$ROOT_DIR/.xianyuassistant.pid"
LOG_FILE="$LOG_DIR/local-start.log"

mkdir -p "$LOG_DIR" "$DB_DIR"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "XianYuAssistant is already running (pid $(cat "$PID_FILE"))."
  exit 0
fi

if lsof -ti tcp:"$PORT" >/dev/null 2>&1; then
  echo "Port $PORT is already in use. Stop the existing process first."
  exit 1
fi

cd "$ROOT_DIR"

export SERVER_PORT="$PORT"
export JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

nohup ./mvnw -q -DskipTests spring-boot:run > "$LOG_FILE" 2>&1 &
APP_PID=$!
echo $APP_PID > "$PID_FILE"

echo "Starting XianYuAssistant on http://127.0.0.1:$PORT"
echo "Log file: $LOG_FILE"
