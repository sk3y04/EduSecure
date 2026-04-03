# PostgreSQL Setup and Security Guide

This guide records the **current verified PostgreSQL posture** of the `EduSecure` repository and the minimum controls that should be described in the final report or followed during deployment.

It is intentionally practical and evidence-based: it distinguishes between what is configured in the repository today and what should be hardened before any shared or production-like environment.

## 1. Current verified repository state

From the current repository contents:

- the backend is configured to use PostgreSQL by default in `backend/src/main/resources/application.properties`
- the PostgreSQL JDBC driver is present in `backend/build.gradle`
- a local PostgreSQL service is defined in `compose.yaml`
- the default runtime JDBC URL is `jdbc:postgresql://localhost:5432/edusecure`
- Liquibase baseline migrations are now defined under `backend/src/main/resources/db/changelog/`
- the main automated test profile still uses **H2 in PostgreSQL compatibility mode** for fast feedback
- the repository now also includes a dedicated PostgreSQL/Testcontainers Liquibase smoke test for real-schema verification

This means:

- PostgreSQL is the intended runtime database
- PostgreSQL wiring exists in the codebase
- runtime schema creation is now versioned through Liquibase rather than relying only on Hibernate schema mutation
- the repository does **not yet prove** production-grade PostgreSQL hardening
- the repository now has a narrow real PostgreSQL verification path, but most of the broader suite still runs against H2

## 2. Local development setup

### 2.1 Compose service

The repository-local database service is defined in `compose.yaml`.

Current defaults:

- database name: `edusecure`
- username: `postgres`
- password: `postgres`
- host port: `5432`
- persistent Docker volume key: `postgres_data`
- effective Docker volume name: `edusecure_dev_postgres_data` by default, overridable with `POSTGRES_DATA_VOLUME_NAME`

These defaults are acceptable only for **single-developer local study use** and should be overridden before any shared deployment.

Persistence behaviour for local development:

- `docker compose up`, `docker compose stop`, and `docker compose down` keep the database contents
- the data is reset only if the named volume is removed explicitly, for example with `docker compose down -v` or `docker volume rm edusecure_dev_postgres_data`
- using an explicit volume name avoids accidental database loss if the Compose project name changes between runs

### 2.2 Backend connection properties

The backend expects these environment variables, with local fallbacks:

| Variable | Current fallback | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/edusecure` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | database username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | database password |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | JDBC driver |
| `SPRING_LIQUIBASE_CHANGE_LOG` | `classpath:db/changelog/db.changelog-master.yaml` | Liquibase changelog location |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | Hibernate mapping validation behaviour |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` | Hibernate dialect |

### 2.3 Local run sequence

Example local workflow:

```powershell
docker compose up -d postgres
Set-Location .\backend
.\gradlew.bat bootRun --no-daemon
```

Example persistence check:

```powershell
docker compose up -d postgres
docker compose exec postgres psql -U postgres -d edusecure -c "CREATE TABLE IF NOT EXISTS dev_persistence_check(id int primary key);"
docker compose down
docker compose up -d postgres
docker compose exec postgres psql -U postgres -d edusecure -c "\dt dev_persistence_check"
```

Intentional reset when you want a clean local database:

```powershell
docker compose down -v
```

If the backend is started from the repository root through Gradle tooling, ensure the same datasource environment variables are available to that process.

### 2.4 Local secrets note

The repository currently uses fallbacks for several security-sensitive values in `application.properties`. For local study work this is convenient, but the project should avoid relying on fallback secrets in any shared environment.

Minimum expectation outside local development:

- inject all secrets through environment variables or a secret store
- do not commit real production credentials
- rotate any credential that may have been exposed in screenshots, notes, or test infrastructure

## 3. What cannot currently be claimed

The following claims should **not** be made from the repository evidence alone:

- that PostgreSQL is "absolutely secure"
- that database traffic is definitely encrypted in transit
- that database storage is definitely encrypted at rest
- that the database delivery process is fully proven across all environments

These are important report wording boundaries.

## 4. Encryption and transport-security assessment

### 4.1 In-transit encryption

The current default JDBC URL does **not** show explicit SSL/TLS enforcement parameters such as:

- `ssl=true`
- `sslmode=require`
- `sslmode=verify-full`
- truststore / certificate configuration

Therefore, the repository does **not currently evidence enforced encrypted transport** between the backend and PostgreSQL.

For a shared or production-like environment, prefer a JDBC URL pattern such as:

```text
jdbc:postgresql://<db-host>:5432/edusecure?sslmode=require
```

If certificates are managed correctly, stronger verification such as `verify-full` is preferable.

### 4.2 At-rest encryption

The current compose setup uses a Docker volume mounted at `/var/lib/postgresql/data`.

This provides persistence, but on its own does **not prove encryption at rest**.

At-rest protection must come from one or more of the following and should be documented explicitly in deployment notes:

- encrypted host disk or partition
- encrypted cloud block storage
- encrypted VM/container host
- infrastructure-level backup encryption

If the final report mentions database confidentiality, it should clearly state whether protection is provided by the application, PostgreSQL, the host platform, or the storage provider.

## 5. Minimum production-hardening recommendations

Before describing any environment as more than local development, the following should be in place.

### 5.1 Credentials and access

- replace `postgres` / `postgres` defaults
- use a dedicated application database user rather than the PostgreSQL superuser where practical
- rotate credentials periodically and after any suspected disclosure
- restrict administrator access to a small trusted set of operators

### 5.2 Network exposure

- avoid exposing PostgreSQL directly to the public internet
- only publish port `5432` when local access genuinely requires it
- in deployment, prefer private networking between the backend and database
- restrict access with firewall rules or container-network segmentation

### 5.3 Transport security

- require TLS/SSL for backend-to-database traffic when the database is not strictly local
- manage trusted certificates explicitly
- document how certificate validation is performed

### 5.4 Backup and recovery

- define scheduled logical or physical backups
- document where backups are stored and whether backup storage is encrypted
- test restore procedures, not just backup creation
- define a retention policy appropriate for the project scope

### 5.5 Runtime configuration

- avoid `spring.jpa.hibernate.ddl-auto=update` in shared or production-like environments
- use explicit migration tooling instead of implicit schema mutation
- keep SQL logging disabled unless it is strictly required for debugging and safe to use

## 6. Testing and verification status

The current default backend test profile in `backend/src/test/resources/application.properties` uses:

- H2 in-memory database
- PostgreSQL compatibility mode
- `spring.docker.compose.enabled=false`

This is useful for fast automated testing, but it does **not** validate:

- PostgreSQL-specific SQL behaviour
- PostgreSQL-specific data types or index behaviour
- real connection/authentication against the configured PostgreSQL service
- TLS-enabled PostgreSQL connection settings

In addition, the repository now has a dedicated PostgreSQL/Testcontainers smoke test that verifies Liquibase schema creation and a minimal repository round-trip against a real PostgreSQL container.

Recommended next step:

- expand the PostgreSQL-backed path from a smoke test into broader repository/API integration coverage where that extra confidence is worth the added test cost

## 7. Schema management and Liquibase status

### 7.1 Current state

The backend now includes a baseline Liquibase changelog for the current schema, and the main runtime configuration has been moved away from Hibernate `ddl-auto=update` toward validation.

This is a meaningful improvement because the schema is now represented in explicit migration files rather than being left entirely to implicit runtime mutation.

### 7.2 What Liquibase now improves

Reasons:

- versioned, reviewable database changesets
- repeatable upgrades across local, test, and deployment environments
- stronger report evidence for disciplined engineering practice
- easier rollback planning and auditability than relying on `ddl-auto=update`

### 7.3 Current limitation

Liquibase is now wired for the main runtime path, while the default automated tests still use H2 with Liquibase disabled for speed and stability.

That limitation is now partially reduced because the repository also includes a dedicated PostgreSQL/Testcontainers smoke test that validates the baseline changelog against a real PostgreSQL instance.

### 7.4 Recommended next adoption step

The next migration-control milestone should happen before:

- multi-developer database sharing
- any production-like deployment
- formal appendix claims about reproducible database delivery

### 7.5 Suggested follow-up path

A sensible next implementation step would be:

1. keep local developer onboarding simple with clear reset instructions
2. add a PostgreSQL-backed integration path using Testcontainers or a dedicated profile
3. add a verification step in CI that starts the app against a clean PostgreSQL schema using Liquibase migrations
4. decide whether reference data such as roles should remain application-seeded or move into managed migrations
5. document how existing non-empty databases should be baseline-synced during adoption

## 8. Suggested report wording

Safe wording:

- "EduSecure is configured to use PostgreSQL as its primary runtime database."
- "Local development uses a Docker Compose PostgreSQL service."
- "The current repository demonstrates PostgreSQL wiring, but not full production hardening."
- "Transport encryption and at-rest storage protection must be explicitly configured and documented for deployment environments."
- "Liquibase is now used as the baseline schema-management mechanism for the main runtime path."

Avoid wording such as:

- "the database is absolutely secure"
- "the database is encrypted" unless the exact mechanism is evidenced
- "the system is production-ready" unless controls are actually implemented and verified

## 9. Quick verification checklist

Use this checklist when validating PostgreSQL setup later:

- [ ] `docker compose config` renders the expected `postgres` service
- [ ] PostgreSQL is reachable only where intended
- [ ] backend startup succeeds against PostgreSQL, not only H2
- [ ] non-default credentials are used outside local development
- [ ] TLS requirements are defined for non-local environments
- [ ] backup and restore procedure is documented
- [ ] schema changes are tracked through Liquibase rather than `ddl-auto=update`
- [ ] Liquibase migrations are exercised against a real PostgreSQL instance, not only H2-backed tests
- [ ] final report language matches verified evidence

