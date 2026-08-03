# Real-Time Event Streaming Platform

A portfolio-oriented event streaming platform built around Java, Spring Boot,
Apache Kafka, Maven, and MySQL. It currently supports HTTP event ingestion,
Kafka publication, transactional consumption, and MySQL persistence.

## Project status

**Current phase:** Phase 5 complete on `phase/05-processing-service`

The Ingestion and Processing Service workflows are implemented. Query,
reliability, observability, and deployment capabilities remain planned for
later phases.

## Build versions

- Java application target: 21
- Spring Boot: 4.1.0
- Maven Wrapper target: 3.9.16

The local build requires JDK 21 through 26. The project compiles application
code for Java 21 so the deployable artifacts do not require the developer's
newer local JDK.

## Building the project

Use the checked-in Maven Wrapper rather than a globally installed Maven:

```bash
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd clean verify
```

The first build downloads the pinned Maven distribution and project
dependencies. Later builds reuse the local Maven cache.

At Phase 1 the modules are intentionally empty. A successful build verifies
the parent-child POM relationships, pinned tool versions, and Maven reactor
order; it does not yet produce runnable Spring Boot services.

The `common-events` module now contains the version 1 immutable order-event
envelope, five typed payloads, validation rules, and JSON contract tests.
The two Spring Boot service modules remain intentionally empty.

## Maven modules

| Module | Packaging | Responsibility |
|---|---|---|
| Root project | `pom` | Aggregates modules and centralizes build rules |
| `common-events` | `jar` | Shared event contracts |
| `ingestion-service` | `jar` | REST ingestion and Kafka production |
| `processing-service` | `jar` | Kafka consumption, persistence, and queries |

`0.1.0-SNAPSHOT` means the project is an unreleased development version.
Maven builds all modules in dependency-aware **reactor order**.

## Local environment file

When local infrastructure is introduced, create a private `.env` from the
tracked template:

```bash
cp .env.example .env
```

Never put cloud credentials, certificates, or real passwords in
`.env.example`. The `.gitignore` excludes the private `.env` file.

## Planned data flow

```text
Client
  |
  v
Ingestion Service
  |
  v
Kafka topic
  |
  v
Processing Service
  |
  v
MySQL
```

The Processing Service will also expose query endpoints for the dashboard.

## Documentation index

- [Complete project plan](docs/PROJECT_PLAN.md)
- [Architecture and technical decisions](docs/ARCHITECTURE.md)
- [Event and data contracts](docs/EVENT_CONTRACT.md)
- [Testing strategy](docs/TESTING.md)
- [Operations, deployment, and troubleshooting](docs/RUNBOOK.md)
- [OpenAPI/Swagger contract](openapi/openapi.yaml)

## Planned repository layout

```text
.
├── common-events/
├── ingestion-service/
├── processing-service/
├── infrastructure/
│   ├── prometheus/
│   └── grafana/
├── docs/
├── openapi/
├── docker-compose.yml
└── pom.xml
```

## Planned technology stack

| Concern | Technology |
|---|---|
| Language | Java |
| Backend framework | Spring Boot |
| Build system | Maven |
| Event broker | Apache Kafka |
| Database | MySQL |
| Local infrastructure | Docker Compose |
| Health and metrics | Spring Boot Actuator and Micrometer |
| Metrics collection | Prometheus |
| Dashboards | Grafana |
| API documentation | OpenAPI 3.0 / Swagger UI |
| Frontend hosting | Cloudflare Pages |
| Backend hosting | Render |
| Managed Kafka and MySQL | Aiven |

## Documentation policy

Architecture, API, event, configuration, or operational behavior changes must
update the relevant document in the same change. Secrets, passwords,
connection strings, private keys, and certificates must never be committed.
