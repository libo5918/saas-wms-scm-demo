#!/usr/bin/env bash
set -euo pipefail

KAFKA_CONTAINER="${1:-kafka-1}"
BOOTSTRAP_SERVER="${2:-kafka-1:29092}"
SALES_ACTUATOR_BASE="${3:-http://localhost:18085}"

SHIP_BAD='{"eventId":"bad-ship-1","orderNo":"SO-BAD-001","status":"SUCCESS"}'
CANCEL_BAD='{"eventId":"bad-cancel-1","orderNo":"SO-BAD-002","status":"FAILED"}'

echo "[1/3] Inject bad ship-result message (missing tenantId)"
docker exec -i "${KAFKA_CONTAINER}" kafka-console-producer --bootstrap-server "${BOOTSTRAP_SERVER}" --topic inventory.ship.result.v1 >/dev/null <<<"${SHIP_BAD}"

echo "[2/3] Inject bad cancel-result message (missing tenantId)"
docker exec -i "${KAFKA_CONTAINER}" kafka-console-producer --bootstrap-server "${BOOTSTRAP_SERVER}" --topic inventory.cancel.result.v1 >/dev/null <<<"${CANCEL_BAD}"

echo "[3/3] Query sales metrics"
echo "ship failed:"
curl -s "${SALES_ACTUATOR_BASE}/actuator/metrics/scm.sales.mq.consume.ship-result.failed" || true
echo
echo "cancel failed:"
curl -s "${SALES_ACTUATOR_BASE}/actuator/metrics/scm.sales.mq.consume.cancel-result.failed" || true
echo
echo "ship dead-letter:"
curl -s "${SALES_ACTUATOR_BASE}/actuator/metrics/scm.sales.mq.consume.ship-result.dead-letter" || true
echo
echo "cancel dead-letter:"
curl -s "${SALES_ACTUATOR_BASE}/actuator/metrics/scm.sales.mq.consume.cancel-result.dead-letter" || true
echo

echo "Done."
