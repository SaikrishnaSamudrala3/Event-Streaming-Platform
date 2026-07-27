# Event Contract

## 1. Purpose

This document describes the planned Kafka event envelope. The Java classes and
JSON serialization tests will become the executable form of this contract.

## 2. Topic contract

| Property | Initial value |
|---|---|
| Main topic | `order.events.v1` |
| Dead-letter topic | `order.events.v1.dlt` |
| Record key | `orderId` |
| Value format | JSON |
| Consumer group | `order-processing-v1` |
| Delivery assumption | At least once |

Topic names must remain configurable rather than hard-coded.

## 3. Event envelope

| Field | Type | Required | Meaning |
|---|---|---|---|
| `eventId` | UUID string | Yes | Globally unique idempotency key |
| `eventType` | Enum | Yes | Order lifecycle event type |
| `eventVersion` | Positive integer | Yes | Schema version for this event |
| `orderId` | String | Yes | Aggregate identifier and Kafka key |
| `occurredAt` | UTC date-time | Yes | Time the business event occurred |
| `source` | String | Yes | System that created the event |
| `correlationId` | String | Yes | Connects related work across services |
| `payload` | JSON object | Yes | Event-specific data |

Constraints in the OpenAPI document apply to HTTP ingestion. Kafka consumers
must also validate messages because Kafka records may originate outside that
endpoint.

## 4. Event types

### `ORDER_CREATED`

Planned payload concepts:

- customer reference using synthetic data
- currency
- total amount
- item summary

### `PAYMENT_COMPLETED`

Planned payload concepts:

- synthetic payment reference
- amount
- currency
- completion timestamp

No card number, CVV, bank credential, or real payment secret is permitted.

### `PAYMENT_FAILED`

Planned payload concepts:

- synthetic payment reference
- safe failure category
- failure timestamp

### `ORDER_CANCELLED`

Planned payload concepts:

- safe cancellation reason
- cancellation timestamp

### `ORDER_SHIPPED`

Planned payload concepts:

- synthetic shipment reference
- carrier name
- shipment timestamp

The exact per-type payload schemas will be finalized before Phase 2 is marked
complete. Until then, the OpenAPI contract intentionally represents `payload`
as an object.

## 5. Versioning

- `eventVersion` begins at `1`.
- Additive, optional fields may remain in the same version when consumers are
  tolerant of unknown fields.
- Removing, renaming, changing meaning, or changing a field type requires a
  versioning decision.
- Consumers should reject or dead-letter versions they cannot safely process.
- Topic version and event schema version are related but not interchangeable.

## 6. Idempotency

- `eventId` identifies one logical event.
- Redelivery with the same `eventId` must not create a second processed record.
- Reusing one `eventId` for different content is invalid and should be
  observable as a contract violation.
- HTTP clients may provide an `Idempotency-Key`; the final mapping between that
  key and `eventId` will be decided during ingestion implementation.

## 7. Time rules

- All timestamps use ISO 8601 with a UTC offset.
- `occurredAt` represents business occurrence time.
- Ingestion time is recorded by the platform.
- Processing time is recorded by the consumer.
- End-to-end latency must state which timestamps it subtracts.

## 8. Dead-letter information

A dead-letter record should preserve the original record when safely possible
and add diagnostic metadata such as:

- original topic
- original partition
- original offset
- failure timestamp
- exception category
- retry count

Stack traces and secrets should remain in controlled logs rather than public
API responses or unsafe event headers.
