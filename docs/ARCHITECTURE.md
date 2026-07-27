# Architecture and Technical Decisions

## 1. System context

The platform accepts order lifecycle events, processes them asynchronously, and
makes their status queryable.

```text
Event sender
    |
    | HTTPS/JSON
    v
Ingestion Service
    |
    | Kafka record keyed by orderId
    v
order.events.v1
    |
    | consumer group
    v
Processing Service
    |
    | transactional database write
    v
MySQL
    ^
    |
Query API <--- Dashboard
```

Prometheus will scrape both services, and Grafana will query Prometheus.

## 2. Why two services

The Ingestion Service and Processing Service have different workloads:

- ingestion scales with incoming HTTP traffic and Kafka publishing
- processing scales with partitions, consumer instances, and processing cost

Separating them demonstrates independent deployment and horizontal scaling
without creating unnecessary services. The query API initially lives in the
Processing Service to keep the first version understandable.

## 3. Processing semantics

The initial guarantee is **at-least-once processing**:

1. Kafka may redeliver a record when processing completes but offset commit
   does not.
2. The database enforces uniqueness on `eventId`.
3. The consumer treats an already stored event as an idempotent duplicate.
4. Offsets are acknowledged only according to the selected error-handling
   strategy.

The project will not claim exactly-once behavior across Kafka and MySQL.

## 4. Partitioning and ordering

`orderId` is the Kafka record key.

Consequences:

- events for the same order are routed consistently
- their order is preserved within a partition
- different orders can be processed in parallel
- hot order IDs can create uneven partition load
- consumers in one group cannot actively exceed the topic partition count

The initial partition count will be selected during local infrastructure work,
then documented with the reason and benchmark implications.

## 5. Reliability flow

```text
Consume event
    |
    v
Validate and process
    |
    +-- success ----------> persist result -> acknowledge
    |
    +-- transient error --> bounded retry with backoff
    |                           |
    |                           +-- recovered -> persist -> acknowledge
    |                           |
    |                           +-- exhausted
    v
Dead-letter topic + observable failure
```

Malformed or incompatible events must not cause an infinite retry loop.

## 6. Consistency boundaries

The initial platform has two separate consistency boundaries:

- HTTP acceptance and Kafka publication
- Kafka consumption and MySQL persistence

A `202 Accepted` response means the ingestion service accepted the request for
asynchronous publication; it does not mean downstream processing completed.
The returned event ID is used to query eventual processing state.

Producer failure behavior and whether an HTTP response waits for broker
acknowledgment will be decided and documented during Ingestion Service
implementation.

## 7. Database model

The processed-event record is expected to contain:

- internal database identifier
- event ID
- event version
- event type
- order ID
- source
- correlation ID
- original event timestamp
- ingestion/received timestamp
- processing timestamp
- processing status
- payload representation
- failure details where safe and appropriate

Payload storage format will be chosen during persistence design. The decision
must balance ease of inspection, indexing needs, and portability on Aiven
MySQL.

## 8. Observability

### Logs

Structured logs should include:

- service name
- event ID
- order ID
- correlation ID
- topic, partition, and offset where applicable
- processing outcome

Secrets and full sensitive payloads must not be logged.

### Metrics

Metrics should answer:

- Is the API available?
- Are events being produced and consumed?
- Is consumer lag growing?
- How long does end-to-end processing take?
- Which failure modes are occurring?
- Is MySQL becoming a bottleneck?

### Health

Liveness answers whether the process is running. Readiness answers whether the
service can perform its role. Health configuration must avoid restart loops
caused by temporary downstream outages.

## 9. Security baseline

- TLS is required for public cloud connections.
- Credentials are injected through environment configuration.
- CORS uses an explicit allowlist.
- Input sizes and validation are bounded.
- Error responses do not expose stack traces or credentials.
- Swagger UI exposure is configurable.
- Management endpoints are minimally exposed.
- No real customer, payment, or personal data is used.

Authentication is deferred, so the deployed demo must be treated as public and
must not provide destructive administrative operations.

## 10. Deployment constraints

Free services may sleep, cold-start, power off after inactivity, or impose
storage, retention, connection, and throughput limits. A background Kafka
consumer hosted on a sleeping web-service plan may not run continuously.

The deployment documentation will record the actual provider behavior observed
at deployment time. Local Docker Compose remains the reliable full-system
demonstration environment.

## 11. Architectural decision log

| ID | Decision | Status | Reason |
|---|---|---|---|
| ADR-001 | Use two deployable backend services | Accepted | Demonstrates independent ingestion and processing without needless complexity |
| ADR-002 | Use a shared Maven event-contract module initially | Accepted | Simple contract consistency while learning; Schema Registry can come later |
| ADR-003 | Key Kafka records by `orderId` | Accepted | Preserves per-order ordering and allows cross-order parallelism |
| ADR-004 | Design for at-least-once delivery | Accepted | Realistic Kafka-to-database semantics without false exactly-once claims |
| ADR-005 | Enforce idempotency with unique `eventId` | Accepted | Makes duplicate delivery safe at the persistence boundary |
| ADR-006 | Put query APIs in Processing Service initially | Accepted | Avoids a third service before its operational value is demonstrated |
| ADR-007 | Use OpenAPI as the REST contract | Accepted | Keeps implementation, tests, frontend, and documentation aligned |
| ADR-008 | Use database migrations | Accepted | Makes schema evolution repeatable locally and in the cloud |
| ADR-009 | Defer authentication | Accepted for first release | Keeps focus on streaming; public demo contains no sensitive operations |
| ADR-010 | Target Java 21 | Accepted | Uses an LTS runtime target that is portable across local and cloud environments |
| ADR-011 | Use Spring Boot 4.1.0 | Accepted | Pins the current stable Spring Boot generation at project creation |
| ADR-012 | Use Maven Wrapper 3.9.16 | Accepted | Makes builds reproducible without requiring a global Maven installation |

New material decisions should receive another ADR row or a dedicated ADR file
if the reasoning is extensive.
