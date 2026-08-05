# ATS Project

Applicant Tracking System — job-service, application-service, and a React
frontend. This is a **Phase 0 skeleton**: project structure and tooling only,
no business logic yet.

## Repo structure

```
ats-project/
  job-service/          Spring Boot — job postings API (skeleton)
  application-service/  Spring Boot — applications API (skeleton)
  frontend/              React + Vite admin/apply UI (skeleton)
  docker/postgres-init/  DB init script run by the postgres container
  docker-compose.yml     Postgres only
```

## Prerequisites

- Java 17 JDK
- Node.js 20.19+ (or 22.12+) and npm
- Docker (with Compose v2)

## 1. Start Postgres

```bash
docker compose up -d
```

This starts one `postgres:18-alpine` container and creates two empty
databases on first boot (via `docker/postgres-init/01-init-databases.sql`):

- `job_service`
- `application_service`

Default credentials are `postgres` / `postgres` on port **`5434`** (not the
standard `5432` — see note below), overridable via a root `.env` file
(`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`) — Compose reads it
automatically, no need to pass `--env-file`.

> **Why port 5434:** this machine already has native PostgreSQL installs
> (`/Library/PostgreSQL/17`, `/18`) bound to `5432` and `5433`. `5434` was
> free. If your machine doesn't have that conflict, feel free to set
> `POSTGRES_PORT=5432` in a root `.env` (and update the `*_DB_PORT` defaults
> in each service's `application.yml`) to use the standard port instead.

To stop it: `docker compose down` (add `-v` to also drop the data volume).

## 2. Run job-service

From the IDE: open `job-service/` and run `JobServiceApplication`.

From the CLI:

```bash
cd job-service
./mvnw spring-boot:run
```

Listens on **http://localhost:8081**. Connection settings read from env vars
(`JOB_SERVICE_DB_HOST`, `JOB_SERVICE_DB_PORT`, `JOB_SERVICE_DB_NAME`,
`JOB_SERVICE_DB_USERNAME`, `JOB_SERVICE_DB_PASSWORD`, `JOB_SERVICE_PORT`),
all defaulted in `application.yml` to match the docker-compose Postgres.

Postgres must be running first — Flyway runs against the real database on
startup (there's no migration content yet, so it's a no-op, but the
connection still has to succeed).

## 3. Run application-service

Same pattern, own port and env var prefix:

```bash
cd application-service
./mvnw spring-boot:run
```

Listens on **http://localhost:8082**. Env vars: `APPLICATION_SERVICE_DB_HOST`,
`APPLICATION_SERVICE_DB_PORT`, `APPLICATION_SERVICE_DB_NAME`,
`APPLICATION_SERVICE_DB_USERNAME`, `APPLICATION_SERVICE_DB_PASSWORD`,
`APPLICATION_SERVICE_PORT`.

## 4. Run the frontend

```bash
cd frontend
npm install
cp .env.example .env   # adjust if your services run on different ports
npm run dev
```

Listens on **http://localhost:5173**. Routes:

- `/apply`
- `/admin/jobs`
- `/admin/applications`

## Verifying the skeleton

1. `docker compose up -d`, then `docker compose ps` — `ats-postgres` should
   be `healthy`.
2. Start job-service, then `curl http://localhost:8081/health` →
   `{"status":"UP"}`.
3. Start application-service, then `curl http://localhost:8082/health` →
   `{"status":"UP"}`.
4. `npm run dev` in `frontend/`, then load `http://localhost:5173/apply`,
   `/admin/jobs`, and `/admin/applications` — each should render a page with
   just its name.
5. `cd job-service && ./mvnw test` and same for `application-service` —
   the one smoke test (context loads) should pass. Requires Postgres to be
   up (step 1), since Flyway connects on context startup.

## What's here vs what's not built yet

**Here:**
- Both services: standard `controller` / `service` / `repository` / `dto` /
  `entity` package layout (empty except `controller`), `/health` endpoint,
  `application.yml` wired to env-var-configured Postgres connections, empty
  `db/migration/` folder, one passing smoke test.
- Frontend: Vite + React + Tailwind, React Router with three placeholder
  routes, empty `pages/` / `components/` / `api/` structure (pages have stub
  content), a bare axios client per service with no calls wired up.
- Postgres in Docker with both databases created on init.

**Not here yet (by design — added by hand as part of the activity plan):**
- Any entity, repository, or CRUD endpoint.
- Flyway migration content.
- Any React component with real state or data fetching.
- Auth, API gateway, Kafka.
- Docker for job-service / application-service themselves.
- Tests beyond the one smoke test per service (e.g. Testcontainers).

## Notes on dependency choices

- **Java 17**: the local JDK was 18.0.2.1 (non-LTS); Spring Boot 4.1's
  Initializr offers 17/21/25/26 as targets, and 17 is the newest one that
  bytecode-runs on JRE 18 without a JDK upgrade.
- **react-router-dom 7.18.2**, not `react-router` 8.x: `react-router@8.3.0`
  requires Node ≥22.22, which the local Node (20.20.2) doesn't satisfy.
  `react-router-dom` 7.x is flagged by `npm audit` for a CSRF advisory, but
  that advisory only affects the unstable RSC APIs, which this skeleton
  (plain `BrowserRouter`/`Routes`) doesn't use.
