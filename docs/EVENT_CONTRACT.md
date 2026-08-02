# Event Contract

## 1. Purpose

This document describes the Kafka event envelope. The Java records in
`common-events` and their JSON serialization tests are the executable form of
this contract.

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

| Field | Type | Required | Rule |
|---|---|---|---|
| `customerReference` | String | Yes | Safe synthetic identifier, maximum 100 characters |
| `currency` | String | Yes | Three-letter uppercase ISO 4217 code |
| `totalAmount` | Decimal | Yes | Greater than zero, maximum two decimal places |
| `itemCount` | Integer | Yes | Greater than zero |

### `PAYMENT_COMPLETED`

| Field | Type | Required | Rule |
|---|---|---|---|
| `paymentReference` | String | Yes | Safe synthetic identifier, maximum 100 characters |
| `amount` | Decimal | Yes | Greater than zero, maximum two decimal places |
| `currency` | String | Yes | Three-letter uppercase ISO 4217 code |
| `completedAt` | UTC date-time | Yes | ISO 8601 string |

No card number, CVV, bank credential, or real payment secret is permitted.

### `PAYMENT_FAILED`

| Field | Type | Required | Rule |
|---|---|---|---|
| `paymentReference` | String | Yes | Safe synthetic identifier, maximum 100 characters |
| `failureReason` | Enum | Yes | One of the safe categories listed below |
| `failedAt` | UTC date-time | Yes | ISO 8601 string |

Safe failure categories:

- `DECLINED`
- `INSUFFICIENT_FUNDS`
- `EXPIRED_PAYMENT_METHOD`
- `PROCESSOR_UNAVAILABLE`
- `VALIDATION_ERROR`
- `UNKNOWN`

### `ORDER_CANCELLED`

| Field | Type | Required | Rule |
|---|---|---|---|
| `reason` | String | Yes | Nonblank safe explanation, maximum 250 characters |
| `cancelledAt` | UTC date-time | Yes | ISO 8601 string |

### `ORDER_SHIPPED`

| Field | Type | Required | Rule |
|---|---|---|---|
| `shipmentReference` | String | Yes | Safe synthetic identifier, maximum 100 characters |
| `carrier` | String | Yes | Nonblank carrier name, maximum 100 characters |
| `shippedAt` | UTC date-time | Yes | ISO 8601 string |

Identifier fields begin with a letter or number and may contain letters,
numbers, dots, underscores, and hyphens. Payloads are immutable Java records
and identify their matching event type. The event envelope rejects a payload
whose type does not match its `eventType`.

## 5. Versioning

- `eventVersion` begins at `1`.
- The `common-events` module currently supports version `1` only.
- Additive, optional fields may remain in the same version when consumers are
  tolerant of unknown fields.
- Removing, renaming, changing meaning, or changing a field type requires a
  versioning decision.
- Consumers should reject or dead-letter versions they cannot safely process.
- Topic version and event schema version are related but not interchangeable.

The envelope and payload records ignore unknown JSON properties so a version
1 consumer can tolerate compatible additive fields. A newly required field is
not an additive compatible change.

## 6. Idempotency

- `eventId` identifies one logical event.
- Redelivery with the same `eventId` must not create a second processed record.
- Reusing one `eventId` for different content is invalid and should be
  observable as a contract violation.
- The Ingestion Service generates a random UUID for every accepted submission.
- The first release does not accept a client idempotency key. Correct conflict
  detection requires a shared durable idempotency store; an in-memory map would
  be incorrect across restarts or horizontally scaled service instances.
- Client-controlled idempotency may be added later with durable request
  fingerprint storage and an explicit expiration policy.

## 7. Time rules

- All timestamps use ISO 8601 with a UTC offset.
- Java represents contract timestamps as `Instant`, and JSON emits them as
  strings such as `2026-07-25T18:30:00Z`.
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
