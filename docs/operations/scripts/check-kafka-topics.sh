#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${1:-kafka-1:29092}"
EXPECTED_PARTITIONS="${2:-3}"
EXPECTED_REPLICATION="${3:-1}"
EXPECTED_MIN_ISR="${4:-1}"

TOPICS=(
  "order.ship.requested.v1"
  "order.cancel.requested.v1"
  "inventory.ship.result.v1"
  "inventory.cancel.result.v1"
  "sales.order.requested.dlq.v1"
  "sales.inventory.result.dlq.v1"
)

echo "Bootstrap: ${BOOTSTRAP_SERVER}"
echo "Expected partitions=${EXPECTED_PARTITIONS}, replication=${EXPECTED_REPLICATION}, min.insync.replicas=${EXPECTED_MIN_ISR}"
echo

check_topic() {
  local topic="$1"
  local describe_output

  if ! describe_output="$(kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --describe --topic "${topic}" 2>/dev/null)"; then
    echo "[FAIL] ${topic}: topic not found"
    return 1
  fi

  local partition_count
  local replication_factor
  partition_count="$(echo "${describe_output}" | sed -n 's/.*PartitionCount: \([0-9]\+\).*/\1/p' | head -n 1)"
  replication_factor="$(echo "${describe_output}" | sed -n 's/.*ReplicationFactor: \([0-9]\+\).*/\1/p' | head -n 1)"

  local min_isr
  min_isr="$(kafka-configs.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --entity-type topics --entity-name "${topic}" --describe 2>/dev/null | sed -n 's/.*min.insync.replicas=\([0-9]\+\).*/\1/p' | head -n 1)"
  min_isr="${min_isr:-1}"

  local ok=0

  if [[ "${partition_count}" != "${EXPECTED_PARTITIONS}" ]]; then
    echo "[FAIL] ${topic}: PartitionCount=${partition_count}, expected=${EXPECTED_PARTITIONS}"
    ok=1
  fi
  if [[ "${replication_factor}" != "${EXPECTED_REPLICATION}" ]]; then
    echo "[FAIL] ${topic}: ReplicationFactor=${replication_factor}, expected=${EXPECTED_REPLICATION}"
    ok=1
  fi
  if [[ "${min_isr}" != "${EXPECTED_MIN_ISR}" ]]; then
    echo "[FAIL] ${topic}: min.insync.replicas=${min_isr}, expected=${EXPECTED_MIN_ISR}"
    ok=1
  fi

  if [[ "${ok}" -eq 0 ]]; then
    echo "[PASS] ${topic}: partitions=${partition_count}, replication=${replication_factor}, min.isr=${min_isr}"
  fi

  return "${ok}"
}

failed=0
for topic in "${TOPICS[@]}"; do
  if ! check_topic "${topic}"; then
    failed=1
  fi
done

echo
if [[ "${failed}" -ne 0 ]]; then
  echo "Topic check failed."
  exit 1
fi

echo "All topic checks passed."
