#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${1:-kafka-1:29092}"
PARTITIONS="${2:-3}"
REPLICATION_FACTOR="${3:-1}"
MIN_ISR="${4:-1}"

TOPICS=(
  "order.ship.requested.v1"
  "order.cancel.requested.v1"
  "inventory.ship.result.v1"
  "inventory.cancel.result.v1"
  "sales.order.requested.dlq.v1"
  "sales.inventory.result.dlq.v1"
)

echo "Bootstrap: ${BOOTSTRAP_SERVER}"
echo "Create topics with partitions=${PARTITIONS}, replication-factor=${REPLICATION_FACTOR}, min.insync.replicas=${MIN_ISR}"
echo

for topic in "${TOPICS[@]}"; do
  echo "Creating topic: ${topic}"
  kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION_FACTOR}" \
    --config "min.insync.replicas=${MIN_ISR}"
done

echo
echo "Done."
echo "You can verify by running:"
echo "bash docs/operations/scripts/check-kafka-topics.sh ${BOOTSTRAP_SERVER} ${PARTITIONS} ${REPLICATION_FACTOR} ${MIN_ISR}"
