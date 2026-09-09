# claims-records-api

A Spring Boot REST API for managing customer records and associated risk flags — modeled as a risk-data records lookup service, the kind of domain problem that shows up in identity verification, fraud/risk, and insurance-claims platforms (e.g. LexisNexis Risk Solutions' core business: linking records and surfacing risk signals against them).

## Stack

- Java 25, Spring Boot 4.1.1
- Spring Data JPA (Hibernate 7) for persistence
- Flyway for versioned schema migrations (`ddl-auto=validate` — Hibernate never auto-generates DDL against this database; every schema change is an explicit, reviewable SQL migration)
- Bean Validation (Jakarta) for request validation
- PostgreSQL 16
- Maven (via the Maven Wrapper, `./mvnw`)

## Why Flyway + `ddl-auto=validate` instead of `ddl-auto=update`

`ddl-auto=update` is fine for a demo but wrong for anything meant to look production-minded: it lets Hibernate silently alter your schema based on entity changes, with no history, no rollback, and no review step. This project keeps schema changes in version-controlled SQL migrations (`src/main/resources/db/migration/V1__create_customer_and_risk_flag_tables.sql`) and configures Hibernate to only *validate* that the entities match the schema Flyway already built — if they drift, the app fails fast at startup instead of drifting silently.

## Architecture

Layered by responsibility rather than by feature, which is the conventional Spring Boot structure and keeps each concern independently testable:

Entities are never returned directly from controllers. Every response is a DTO record built explicitly in the service layer — this avoids leaking JPA lazy-loading proxies into JSON serialization (a common source of bugs and `LazyInitializationException`s in less careful Spring APIs) and keeps the API contract decoupled from the database schema.

## Data model

- `Customer` — has many `RiskFlag`s (`@OneToMany`, cascade + orphan removal, so deleting a customer cleans up its flags)
- `RiskFlag` — belongs to a `Customer` (`@ManyToOne`, lazy-loaded), with a `Severity` (LOW/MEDIUM/HIGH/CRITICAL) and a `Status` (OPEN/RESOLVED/DISMISSED), stored as strings via `@Enumerated(EnumType.STRING)` so the database stays human-readable and resilient to enum reordering

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/customers` | Create a customer |
| GET | `/api/customers` | List customers (paginated) |
| GET | `/api/customers/{id}` | Get a customer |
| DELETE | `/api/customers/{id}` | Delete a customer (cascades to their risk flags) |
| POST | `/api/customers/{id}/risk-flags` | Add a risk flag to a customer |
| GET | `/api/customers/{id}/risk-flags` | List a customer's risk flags |
| GET | `/api/customers/{id}/risk-summary` | Aggregate open-flag counts by severity |

## Real captured examples

Create a customer:

```json
{
    "id": 1,
    "fullName": "Jordan Espinosa",
    "email": "jordan.espinosa@example.com",
    "createdAt": "2026-09-09T15:56:16.947413",
    "openRiskFlagCount": 0
}
```

Add a risk flag:

```json
{
    "id": 1,
    "customerId": 1,
    "category": "Identity Verification",
    "severity": "HIGH",
    "status": "OPEN",
    "notes": "SSN mismatch flagged during onboarding",
    "createdAt": "2026-09-09T15:59:30.742049"
}
```

Aggregated risk summary after adding two flags (HIGH and MEDIUM):

```json
{
    "customerId": 1,
    "openFlagCount": 2,
    "openBySeverity": {
        "HIGH": 1,
        "MEDIUM": 1
    }
}
```

Validation failure — invalid email, caught before touching the database:

Duplicate email conflict:

```json
{
    "error": "Conflict",
    "message": "A customer with email jordan.espinosa@example.com already exists",
    "timestamp": "2026-09-09T15:56:54.743569",
    "status": 409
}
```

Not-found for a nonexistent customer:

## Running locally

Flyway applies the schema automatically on startup; no manual SQL needed.

## Error handling design

A single `@RestControllerAdvice` (`GlobalExceptionHandler`) maps every failure mode to a structured JSON error body instead of leaking a stack trace: bean-validation failures to `400` with a `fieldErrors` map, `ResourceNotFoundException` to `404`, `DuplicateEmailException` to `409`. This centralizes error formatting in one place rather than scattering try/catch blocks across every controller method.
