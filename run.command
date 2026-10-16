#!/bin/sh
cd "$(dirname "$0")" || exit 1
if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 or newer is required. Download it from https://adoptium.net"
  exit 1
fi
exec java -jar WordSurvive.jar "$@"
