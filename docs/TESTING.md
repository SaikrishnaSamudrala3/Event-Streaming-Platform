# Testing and Benchmark Strategy

## 1. Principles

- Test behavior at the smallest useful level.
- Use real Kafka and MySQL-compatible behavior for integration boundaries.
- Keep tests deterministic and independently repeatable.
- Do not use benchmark numbers in documentation or project claims until the
  raw procedure and results exist.
- Functional correctness takes priority over throughput.

## 2. Test layers

### Shared-contract tests

- JSON serialization and deserialization
- required fields
- event-type handling
- timestamp format
- forward-compatible unknown-field behavior

### Ingestion unit tests

- request validation
- request-to-event mapping
- correlation and event identifier behavior
- producer success and failure handling

### Ingestion API tests

- accepted request
- malformed JSON
- missing required fields
- invalid enum and timestamp
- oversized or invalid fields
- standard error response
- content type

### Processing unit tests

- event dispatch by type
- duplicate-event behavior
- status transitions
- transient versus non-retryable error classification

### Persistence tests

- migrations apply cleanly
- event ID uniqueness
- indexes support planned queries
- transaction rollback
- pagination and filtering

### Kafka integration tests

- producer record key and topic
- consumer group processing
- partition behavior
- offset acknowledgment
- retry and dead-letter routing
- malformed record behavior

### End-to-end tests

- POST event, consume it, store it, and query it
- duplicate submission
- database interruption
- consumer restart
- invalid payload
- exhausted retry

Testcontainers will be considered for Kafka and MySQL integration tests once
the basic services are working.

### Phase 5 verification completed

- The repository unit and application-context suites cover event mapping,
  persistence decisions, duplicate outcomes, and listener acknowledgment.
- A real local end-to-end check covered HTTP ingestion, Kafka publication,
  transactional consumption, and MySQL persistence.
- Republishing the exact event advanced the consumer-group offset while the
  event-ID row count remained one.
- Restarting the consumer resumed at the committed offset without replaying
  the acknowledged records.

Repeatable container-managed integration tests remain planned. They should use
isolated topics, consumer groups, and database state so normal local data does
not influence their results.

## 3. API contract testing

The implementation must match `openapi/openapi.yaml`:

- paths and HTTP methods
- request and response fields
- status codes
- validation limits
- error representation
- pagination shape

Contract drift is a defect. The Ingestion Service tests verify its generated
OpenAPI operation, response statuses, request header, payload alternatives, and
environment-specific documentation exposure. Broader repository-contract
validation will expand as the later endpoints are implemented.

## 4. Benchmark questions

Benchmarks should answer:

- What event rate can ingestion sustain?
- What rate can the consumer persist?
- What are p50, p95, and p99 end-to-end latencies?
- At what rate does consumer lag grow?
- Does adding consumer instances improve throughput?
- When does MySQL become the limiting component?
- How do retries affect throughput and latency?

## 5. Benchmark scenarios

1. Baseline: one partition and one consumer.
2. Partition scaling: increase partitions with one consumer instance.
3. Consumer scaling: multiple instances in the same group.
4. Sustained traffic: fixed rate over a meaningful duration.
5. Burst traffic: short input spike followed by recovery.
6. Slow database: increased write latency or constrained connections.
7. Failure injection: selected processing failures and retries.

## 6. Required benchmark record

Every saved result must include:

- date and code revision
- local or cloud environment
- CPU, memory, and operating system
- Java version
- Kafka and MySQL versions
- topic partition count
- consumer instance and concurrency counts
- producer acknowledgment and batching configuration
- database connection-pool configuration
- event payload size
- test duration and warm-up
- offered and achieved event rates
- success, retry, and failure counts
- latency percentiles
- starting, peak, and ending consumer lag

## 7. Completion gate

Before a release or deployment:

- Maven tests pass.
- Integration tests pass.
- OpenAPI validation passes.
- Docker Compose smoke test passes.
- No secrets are present in tracked files.
- Health and metrics endpoints behave as documented.
- The runbook is updated for new failure modes.
