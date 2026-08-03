#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"

if ! docker info >/dev/null 2>&1; then
    echo "Docker is unavailable. Start Docker Desktop and try again." >&2
    exit 1
fi

if [[ ! -f .env ]]; then
    echo "Missing .env. Copy .env.example to .env and configure local passwords." >&2
    exit 1
fi

echo "Validating Docker Compose configuration..."
docker compose config --quiet

echo "Starting Kafka and MySQL and waiting for health checks..."
docker compose up -d --wait --wait-timeout 120 kafka mysql

echo "Creating required Kafka topics when absent..."
docker compose run --rm kafka-init >/dev/null

read_env_setting() {
    local setting_name="$1"
    local default_value="$2"
    local configured_value

    configured_value="$(sed -n "s/^${setting_name}=//p" .env | tail -n 1)"
    printf '%s' "${configured_value:-$default_value}"
}

expected_partitions="$(read_env_setting KAFKA_DEFAULT_PARTITIONS 3)"
main_topic="$(read_env_setting KAFKA_ORDER_EVENTS_TOPIC order.events.v1)"
dlt_topic="$(read_env_setting KAFKA_ORDER_EVENTS_DLT_TOPIC order.events.v1.dlt)"

verify_topic() {
    local topic_name="$1"
    local description

    description="$(docker compose exec -T kafka \
        /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --describe \
        --topic "$topic_name")"

    if [[ "$description" != *"PartitionCount: $expected_partitions"* ]]; then
        echo "Topic $topic_name does not have $expected_partitions partitions." >&2
        exit 1
    fi

    if [[ "$description" != *"ReplicationFactor: 1"* ]]; then
        echo "Topic $topic_name does not have replication factor 1." >&2
        exit 1
    fi

    echo "Verified topic $topic_name ($expected_partitions partitions, replication factor 1)."
}

verify_topic "$main_topic"
verify_topic "$dlt_topic"

smoke_id="phase4-smoke-$(date +%s)"
smoke_record="$smoke_id:{\"check\":\"kafka-connectivity\",\"result\":\"ok\"}"

echo "Producing and consuming Kafka smoke record $smoke_id..."
printf '%s\n' "$smoke_record" | docker compose exec -T kafka \
    /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 \
    --topic "$main_topic" \
    --reader-property parse.key=true \
    --reader-property key.separator=: >/dev/null

docker compose exec -T kafka sh -c '
    /opt/kafka/bin/kafka-console-consumer.sh \
        --bootstrap-server localhost:9092 \
        --topic "$1" \
        --from-beginning \
        --timeout-ms 10000 \
        --formatter-property print.key=true \
        --formatter-property key.separator=: 2>/dev/null \
        | grep -F -m 1 "$2" >/dev/null
' shell "$main_topic" "$smoke_id"

echo "Verifying MySQL application-user connectivity..."
docker compose exec -T mysql sh -c '
    MYSQL_PWD="$MYSQL_PASSWORD" mysql \
        --user="$MYSQL_USER" \
        --database="$MYSQL_DATABASE" \
        --batch \
        --skip-column-names \
        --execute="SELECT 1" 2>/dev/null \
        | grep -Fx "1" >/dev/null
'

echo "Local infrastructure verification passed."
