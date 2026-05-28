#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${SERVER_PORT:-12400}"
PID_FILE="$ROOT_DIR/.xianyuassistant.pid"

if [ -f "$PID_FILE" ]; then
  PID="$(cat "$PID_FILE")"
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    echo "Stopped process $PID."
  fi
  rm -f "$PID_FILE"
fi

PIDS="$(lsof -ti tcp:"$PORT" 2>/dev/null || true)"
if [ -n "$PIDS" ]; then
  kill $PIDS || true
  echo "Stopped processes using port $PORT."
fi

echo "XianYuAssistant is stopped."
