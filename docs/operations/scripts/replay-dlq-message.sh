#!/usr/bin/env bash
set -euo pipefail

KAFKA_CONTAINER="${1:-kafka-1}"
BOOTSTRAP_SERVER="${2:-kafka-1:29092}"
DLQ_TOPIC="${3:-sales.inventory.result.dlq.v1}"

echo "Usage:"
echo "  bash docs/operations/scripts/replay-dlq-message.sh <kafka_container> <bootstrap_server> <dlq_topic>"
echo
echo "Example:"
echo "  bash docs/operations/scripts/replay-dlq-message.sh kafka-1 kafka-1:29092 sales.inventory.result.dlq.v1"
echo
echo "Step 1: consume one DLQ message manually and extract sourceTopic + value"
echo "Command:"
echo "  docker exec -it ${KAFKA_CONTAINER} kafka-console-consumer --bootstrap-server ${BOOTSTRAP_SERVER} --topic ${DLQ_TOPIC} --from-beginning --max-messages 1"
echo
echo "Step 2: paste corrected JSON payload for replay."
echo
read -r -p "Source topic to replay (e.g. inventory.ship.result.v1): " SOURCE_TOPIC
read -r -p "Replay message key (can be empty): " MESSAGE_KEY
echo "Paste corrected JSON payload in one line, then press Enter:"
read -r CORRECTED_PAYLOAD

if [[ -z "${SOURCE_TOPIC}" || -z "${CORRECTED_PAYLOAD}" ]]; then
  echo "source topic and payload are required."
  exit 1
fi

if [[ -n "${MESSAGE_KEY}" ]]; then
  printf "%s:%s\n" "${MESSAGE_KEY}" "${CORRECTED_PAYLOAD}" | \
    docker exec -i "${KAFKA_CONTAINER}" kafka-console-producer \
      --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --topic "${SOURCE_TOPIC}" \
      --property "parse.key=true" \
      --property "key.separator=:"
else
  printf "%s\n" "${CORRECTED_PAYLOAD}" | \
    docker exec -i "${KAFKA_CONTAINER}" kafka-console-producer \
      --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --topic "${SOURCE_TOPIC}"
fi

echo "Replay message sent to ${SOURCE_TOPIC}."
