# Operations, Deployment, and Troubleshooting Runbook

This runbook will evolve as commands, service names, ports, and provider
configuration are implemented. It intentionally avoids inventing commands
before the corresponding files exist.

## 1. Environment model

### Local

- Spring Boot services run from Maven or containers.
- Kafka, MySQL, Prometheus, and Grafana run through Docker Compose.
- Safe development defaults may be used.

### Cloud demo

- React: Cloudflare Pages
- Spring Boot services: Render
- Kafka: Aiven
- MySQL: Aiven
- Prometheus and Grafana: initially local only

Free-tier services may sleep or power off after inactivity. The live demo may
experience cold starts and should not be described as continuously available
production infrastructure.

## 2. Configuration checklist

Before starting a service, verify:

- correct application profile
- service port is available
- Kafka bootstrap servers are reachable
- Kafka security protocol matches the environment
- required certificates or secrets are mounted/injected
- topic names are correct
- consumer group ID is correct
- database URL and credentials are correct
- database migration permissions are available
- CORS origins match the frontend
- management endpoints have the intended exposure

## 3. Startup order

Recommended local order:

1. Start Kafka and MySQL.
2. Wait for infrastructure health checks.
3. Start Processing Service.
4. Start Ingestion Service.
5. Submit a smoke-test event.
6. Query the event status.
7. Start Prometheus and Grafana when observability is enabled.

Applications should still fail clearly and recover sensibly when dependencies
start in a different order.

## 4. Smoke-test expectations

A successful smoke test will eventually verify:

1. Ingestion health is ready.
2. Processing health is ready.
3. `POST /api/v1/events` returns `202`.
4. The response contains an event ID.
5. The consumer processes the event.
6. `GET /api/v1/events/{eventId}` returns the stored result.
7. Metrics show published and processed events.
8. Logs can be correlated using the event or correlation ID.

## 5. Troubleshooting map

### API cannot be reached

Check:

- process status and port
- application startup logs
- Render cold start
- health endpoint
- CORS only if the browser fails but direct API access works

### API returns `202`, but no record appears

Check in order:

- producer success/failure log for the event ID
- correct topic name
- Kafka broker connectivity
- consumer process status
- consumer group and subscription
- consumer lag
- deserialization or validation errors
- retry activity
- dead-letter topic
- MySQL connectivity and transaction errors

### Consumer lag continuously grows

Possible causes:

- offered rate exceeds processing rate
- insufficient partitions or active consumers
- slow database writes
- exhausted database connection pool
- long processing logic
- retry storm
- consumer rebalances

Record input rate, processing rate, partition assignment, database latency, and
retry count before changing configuration.

### Duplicate database records

Check:

- unique constraint on event ID
- transaction boundary
- idempotency lookup/insert behavior
- whether callers reused content with a different event ID

Kafka redelivery itself is expected under at-least-once processing.

### Events for one order are out of order

Check:

- producer uses `orderId` as the record key
- the topic was not changed
- records are compared within the same partition
- retries or application-level concurrency are not reordering side effects

Kafka does not guarantee global ordering across partitions.

### Consumer repeatedly processes a bad record

Check:

- exception classification
- retry limit and backoff
- dead-letter publishing
- offset acknowledgment after dead-letter recovery
- deserializer error handling

The system must avoid an infinite poison-message loop.

### MySQL connection failures

Check:

- hostname, port, database name, and TLS requirements
- credentials and secret injection
- Aiven service activity
- connection limits and pool size
- network allowlists, if configured
- migration failure before application startup

### Kafka TLS/authentication failures

Check:

- bootstrap address
- security protocol
- CA certificate
- client certificate and key or configured credentials
- file mounting and permissions
- accidental newline or quoting problems in environment secrets

Never print private keys or passwords while diagnosing.

### Metrics missing from Prometheus

Check:

- Actuator endpoint exposure
- Prometheus registry dependency
- scrape target and port
- network path from Prometheus
- scrape status and error
- metric name changes

### Swagger UI or OpenAPI is unavailable

Check:

- documentation dependency and configuration
- environment-specific enablement
- proxy path handling
- whether only the repository contract exists at the current project phase

## 6. Failure-response discipline

For any material incident or bug, capture:

- observed symptom
- time and environment
- event/correlation IDs
- affected service
- relevant topic, partition, and offset
- expected versus actual behavior
- logs and metrics without secrets
- reproduction steps
- root cause
- fix and verification
- documentation or test added to prevent recurrence

## 7. Deployment checklist

- Run all tests.
- Validate the OpenAPI document.
- Confirm database migrations.
- Confirm topic configuration.
- Verify cloud services are active.
- Inject secrets through provider configuration.
- Confirm TLS.
- Restrict CORS.
- Confirm health endpoints.
- Deploy Processing Service before sending events.
- Deploy Ingestion Service.
- Run the smoke test.
- Review logs, errors, lag, and database records.
- Record free-tier limitations and observed cold-start behavior.

## 8. Rollback approach

Until a formal deployment pipeline exists:

- retain the last known-good application artifact or deployment
- make database migrations backward-aware
- avoid destructive migrations in the same release as code changes
- stop event ingestion if downstream processing would corrupt data
- preserve Kafka records within available retention
- document any manual replay procedure before using it

Rollback must not blindly replay events without idempotency protection.

## 9. Documentation maintenance

Update this runbook whenever:

- a new dependency or environment variable is introduced
- startup or deployment steps change
- a new failure mode is discovered
- a metric or health check changes
- retry, dead-letter, or offset behavior changes
- a troubleshooting session reveals a reusable diagnostic
