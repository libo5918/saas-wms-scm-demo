#!/usr/bin/env bash
set -euo pipefail

USE_DOCKER="${USE_DOCKER:-true}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-kafka-1}"
CURL_TIMEOUT_SECONDS="${CURL_TIMEOUT_SECONDS:-5}"
SERVICE_HOST="host.docker.internal"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --docker)
      USE_DOCKER="true"
      shift
      ;;
    --container)
      KAFKA_CONTAINER="${2:?missing container name}"
      shift 2
      ;;
    --host)
      SERVICE_HOST="${2:?missing host}"
      shift 2
      ;;
    --help|-h)
      echo "Usage: preflight-check.sh [--docker] [--container <name>] [--host <service_host>] [bootstrap partitions replication min_isr [inventory_base sales_base]]"
      exit 0
      ;;
    *)
      break
      ;;
  esac
done

BOOTSTRAP_SERVER="${1:-kafka-1:29092}"
EXPECTED_PARTITIONS="${2:-3}"
EXPECTED_REPLICATION="${3:-3}"
EXPECTED_MIN_ISR="${4:-2}"
INVENTORY_BASE="${5:-http://${SERVICE_HOST}:18084}"
SALES_BASE="${6:-http://${SERVICE_HOST}:18085}"

ok() { echo "[PASS] $1"; }
fail() { echo "[FAIL] $1"; }

check_http() {
  local name="$1"
  local url="$2"
  if curl -fsS --max-time "${CURL_TIMEOUT_SECONDS}" "$url" >/dev/null; then
    ok "$name $url"
  else
    fail "$name $url"
    return 1
  fi
}

echo "== 1) Kafka topic contract check =="
USE_DOCKER="${USE_DOCKER}" KAFKA_CONTAINER="${KAFKA_CONTAINER}" bash docs/operations/scripts/check-kafka-topics.sh \
  "$BOOTSTRAP_SERVER" "$EXPECTED_PARTITIONS" "$EXPECTED_REPLICATION" "$EXPECTED_MIN_ISR"

echo
echo "== 2) Service health check =="
check_http "inventory health" "${INVENTORY_BASE}/actuator/health"
check_http "sales health" "${SALES_BASE}/actuator/health"

echo
echo "== 3) Metrics endpoint check =="
check_http "inventory metrics" "${INVENTORY_BASE}/actuator/metrics"
check_http "sales metrics" "${SALES_BASE}/actuator/metrics"

echo
echo "== 4) Key metric presence check =="
check_http "inventory consume.failed" "${INVENTORY_BASE}/actuator/metrics/scm.inventory.mq.consume.failed"
check_http "inventory consume.dead-letter" "${INVENTORY_BASE}/actuator/metrics/scm.inventory.mq.consume.dead-letter"
check_http "sales ship-result.failed" "${SALES_BASE}/actuator/metrics/scm.sales.mq.consume.ship-result.failed"
check_http "sales cancel-result.failed" "${SALES_BASE}/actuator/metrics/scm.sales.mq.consume.cancel-result.failed"

echo
ok "Preflight check passed. Environment is ready for continued development."
