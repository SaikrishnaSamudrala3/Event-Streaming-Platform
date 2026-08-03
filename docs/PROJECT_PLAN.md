# Project Plan

## 1. Objective

Build a real-time order-event streaming platform that demonstrates:

- REST event ingestion
- asynchronous Kafka producer-consumer processing
- partition-based parallelism and consumer groups
- durable MySQL persistence
- query APIs for a future React dashboard
- retry, dead-letter, and idempotency behavior
- measurable throughput, latency, failures, and consumer lag
- reproducible local development and cloud deployment

This is a learning and portfolio project. It is not intended to claim
production readiness or guaranteed availability on free hosting tiers.

## 2. Scope

### Included

- Order-domain events
- Ingestion and Processing Spring Boot services
- Shared Java event contract
- Kafka topics and partitioning
- MySQL persistence and migrations
- REST query API
- OpenAPI documentation and Swagger UI
- Docker Compose local environment
- Actuator, Micrometer, Prometheus, and Grafana
- Automated tests
- Aiven, Render, and Cloudflare deployment documentation
- A repeatable load-test and benchmark procedure

### Deferred until the core platform works

- User accounts and authentication
- Authorization and roles
- Kubernetes
- Kafka Connect
- Schema Registry
- distributed tracing
- multiple databases
- a large collection of artificial microservices
- exactly-once processing claims

### Explicitly out of scope for the first release

- Real payment processing
- Personally identifiable or confidential data
- Production service-level agreements
- Unlimited event retention
- Multi-region disaster recovery

## 3. Domain

The first domain is an e-commerce order lifecycle:

- `ORDER_CREATED`
- `PAYMENT_COMPLETED`
- `PAYMENT_FAILED`
- `ORDER_CANCELLED`
- `ORDER_SHIPPED`

The platform receives an event, validates its envelope, publishes it to Kafka,
processes it asynchronously, and stores its processing result. Event payloads
are illustrative and must not contain real payment credentials.

## 4. Services

### Common Events module

Owns the shared event envelope, event-type enumeration, payload models, and
serialization contract.

### Ingestion Service

Responsibilities:

- expose `POST /api/v1/events`
- validate the HTTP request
- generate or accept safe event metadata
- publish using `orderId` as the Kafka key
- return `202 Accepted` with the event identifier
- expose health and metrics

It does not store the final processed event in MySQL.

### Processing Service

Responsibilities:

- consume events as part of a Kafka consumer group
- validate and process events
- prevent duplicate persistence by event ID
- store processing results in MySQL
- expose event query and statistics endpoints
- retry transient failures
- route exhausted failures to a dead-letter topic
- expose health and metrics

## 5. Planned repository structure

```text
.
├── pom.xml
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── common-events/
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       └── test/java/
├── ingestion-service/
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       └── test/java/
├── processing-service/
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       └── test/java/
├── infrastructure/
│   ├── prometheus/
│   └── grafana/
├── docs/
└── openapi/
```

Package names will be selected once and used consistently. Code will be
organized by feature or clear application layer; controllers will not contain
business logic and Kafka listeners will delegate to services.

## 6. Delivery phases

### Phase 0 — Documentation and contracts

Files:

- `README.md`
- `docs/PROJECT_PLAN.md`
- `docs/ARCHITECTURE.md`
- `docs/EVENT_CONTRACT.md`
- `docs/TESTING.md`
- `docs/RUNBOOK.md`
- `openapi/openapi.yaml`

Exit criteria:

- Scope and terminology are consistent.
- Planned REST operations are described by valid OpenAPI.
- Major reliability and deployment assumptions are recorded.

### Phase 1 — Maven foundation

Status: **Complete**

Files:

- root `pom.xml`
- `.gitignore`
- `.env.example`
- module `pom.xml` files
- initial module directory structure
- Maven Wrapper scripts and configuration

Exit criteria:

- The Maven reactor detects every module.
- A clean build succeeds.
- Java and Spring Boot versions are pinned.

### Phase 2 — Shared event contract

Status: **Complete**

Planned files:

- `OrderEvent`
- `OrderEventType`
- payload models
- serialization tests

Implemented additions:

- sealed payload contract
- contract version helper
- shared contract validation
- typed payloads for all five order event types
- safe payment failure categories
- JSON shape, compatibility, and validation tests
- synchronized OpenAPI payload schemas

Exit criteria:

- Required fields are defined.
- JSON round-trip serialization passes.
- Event versioning rules are documented and tested.

### Phase 3 — Ingestion Service

Status: **Complete**

Planned components:

- application entry point
- REST request and response models
- controller
- mapper
- producer service
- Kafka producer configuration
- validation
- global exception handling
- configuration profiles
- generated OpenAPI documentation and Swagger UI
- unit and controller tests

Implemented additions:

- versioned `POST /api/v1/events` ingestion endpoint
- typed request mapping to the shared event contract
- Kafka publication keyed by `orderId`
- broker-acknowledged `202 Accepted` workflow with a bounded timeout
- standard problem-details responses for validation, publication, and routing failures
- local, test, and cloud configuration profiles with environment-driven settings
- explicit CORS allowlist configuration
- generated OpenAPI and Swagger UI with environment-specific exposure
- regression coverage for payload schemas and disabled documentation routes

Exit criteria:

- Valid requests receive `202 Accepted`.
- Invalid requests receive the standard error body.
- A valid request produces a Kafka record keyed by `orderId`.
- No secrets exist in source control.

### Phase 4 — Local infrastructure

Status: **Complete**

Planned components:

- Kafka service
- MySQL service
- named volumes where appropriate
- health checks
- documented environment variables

Implemented additions:

- pinned single-node Apache Kafka 4.2.0 broker in KRaft mode
- pinned MySQL 8.4 database with a limited application user
- separate host and container Kafka listeners
- health checks for Kafka and MySQL
- idempotent creation of the main and dead-letter topics
- three partitions per topic with replication factor one for local development
- named Kafka and MySQL data volumes with verified persistence
- repeatable Kafka and MySQL infrastructure verification script
- documented startup, inspection, restart, cleanup, and troubleshooting commands

Exit criteria:

- Infrastructure starts through Docker Compose.
- Service connectivity can be verified.
- Restart and cleanup procedures are documented.

### Phase 5 — Processing Service

Status: **Complete**

Planned components:

- application entry point
- Kafka consumer configuration
- listener
- processing service
- persistence entity and repository
- database migrations
- consumer and persistence tests

Exit criteria:

- [x] Published events are consumed.
- [x] Successfully processed events are stored in MySQL.
- [x] Event ID uniqueness prevents duplicate rows.
- [x] Offset acknowledgment behavior is tested.

Completion verification used the real local Kafka and MySQL services. A newly
published HTTP event was persisted with its Kafka coordinates, an exact Kafka
redelivery returned `DUPLICATE` without adding a row, the committed offset
advanced, and a consumer restart resumed from that committed offset.

### Phase 6 — Query API

Planned operations:

- retrieve an event by event ID
- list events with pagination
- filter by order ID, type, and processing status
- retrieve basic processing statistics

Exit criteria:

- Responses match the OpenAPI contract.
- Pagination is bounded.
- Missing records and invalid filters use the standard error response.

### Phase 7 — Reliability

Planned capabilities:

- idempotent consumer behavior
- bounded retry with backoff
- dead-letter topic
- failure metadata
- malformed-message handling
- correlation IDs
- structured logs

Exit criteria:

- Duplicate delivery does not duplicate stored results.
- Transient failures retry a bounded number of times.
- Exhausted failures reach the dead-letter topic.
- Failure behavior is covered by integration tests.

### Phase 8 — Observability

Planned capabilities:

- Actuator health and readiness
- Prometheus metrics endpoint
- custom Micrometer counters and timers
- Prometheus configuration
- Grafana dashboards

Key signals:

- published, processed, retried, failed, and dead-letter event counts
- end-to-end event latency
- consumer processing duration
- consumer lag
- API latency and errors
- database-operation latency

Exit criteria:

- Prometheus scrapes both applications locally.
- Grafana displays the key signals.
- Metric names and meanings are documented.

### Phase 9 — End-to-end testing and benchmarking

Planned capabilities:

- integration environment using Testcontainers where suitable
- deterministic sample-event generator
- repeatable load-test scenarios
- captured environment and hardware information
- p50, p95, and p99 latency reporting

Exit criteria:

- The full ingestion-to-database path is automatically verified.
- Benchmark claims are reproducible and based on saved results.
- Scaling is compared across consumer counts and partition counts.

### Phase 10 — Cloud deployment

Planned targets:

- Aiven Kafka
- Aiven MySQL
- Render Spring Boot services
- Cloudflare Pages frontend later

Exit criteria:

- TLS connectivity works without committed credentials.
- Health endpoints are available.
- CORS is restricted to configured origins.
- Free-tier sleep, retention, storage, and throughput limitations are noted.
- A smoke test verifies the live ingestion and query path.

## 7. API rules

- API prefix: `/api/v1`
- JSON is the default representation.
- Timestamps use UTC ISO 8601.
- Event identifiers are UUIDs.
- Event creation is asynchronous and returns HTTP `202`.
- Errors follow one shared error schema.
- List endpoints use bounded page-number pagination.
- Breaking API changes require a new API version.
- Swagger UI will be exposed only as appropriate for each environment.

The authoritative planned REST contract is
[`openapi/openapi.yaml`](../openapi/openapi.yaml).

## 8. Kafka rules

- Initial main topic: `order.events.v1`
- Initial dead-letter topic: `order.events.v1.dlt`
- Kafka record key: `orderId`
- Initial consumer group: `order-processing-v1`
- Ordering is promised only for records in the same partition.
- The design assumes at-least-once delivery.
- Consumers must be idempotent.
- Partition count is a deployment decision and controls maximum active
  consumer parallelism within one group.
- Retention is not a substitute for database persistence.

## 9. Database rules

Initial logical tables:

- processed event records
- processing failure records, if failure persistence is retained

Rules:

- Event ID has a unique constraint.
- Schema changes use versioned migrations.
- Hibernate automatic schema creation is not used in production.
- API models and persistence entities remain separate.
- Database credentials come from environment configuration.
- Indexes will be added based on query paths: event ID, order ID, type,
  processing status, and received timestamp.
- Event payloads use native MySQL JSON storage while queryable envelope fields
  remain relational columns.
- Kafka topic, partition, and offset form a unique source-position constraint.
- Event and processing timestamps use microsecond-precision `DATETIME(6)` and
  are written as UTC by the application.

## 10. Configuration and secrets

Configuration will use environment variables with safe local defaults where
reasonable. Expected categories include:

- server ports
- Kafka bootstrap servers
- Kafka security protocol
- Kafka certificate locations or secret values
- topic names
- consumer group ID
- database URL and credentials
- CORS origins
- retry settings
- management endpoint exposure

Only variable names and examples belong in `.env.example`. Real `.env` files,
certificates, keystores, passwords, and cloud connection strings are ignored.

## 11. Definition of done

A feature is complete only when:

- behavior is implemented
- relevant tests pass
- errors and edge cases are considered
- configuration is documented
- OpenAPI is updated when REST behavior changes
- event documentation is updated when the Kafka contract changes
- operational impact is reflected in the runbook
- no secrets or generated build artifacts are committed

## 12. Working method

We will work one small phase at a time:

1. State the files to create or change.
2. Explain each file’s responsibility.
3. Implement only that slice.
4. Run focused verification.
5. Review the result.
6. Update documentation when decisions change.
7. Continue only after the current slice is stable.
