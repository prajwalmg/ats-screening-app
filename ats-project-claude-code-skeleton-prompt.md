# Claude Code Prompt — ATS Project Skeleton Scaffold (Phase 0)

Paste this into Claude Code. This is deliberately a **skeleton only** — no
business logic, no entities beyond a placeholder, no CRUD. I'm building the
actual functionality myself as hands-on practice (see my activity plan);
Claude Code's job here is just to get the project structure, tooling, and
plumbing in place so I'm editing real files instead of starting from zero.

## What to build

### Repo structure
```
ats-project/
  job-service/          (Spring Boot)
  application-service/  (Spring Boot)
  frontend/              (React, Vite)
  docker-compose.yml
  README.md
```

### 1. job-service (Spring Boot)
- Spring Initializr equivalent setup: Spring Web, Spring Data JPA, PostgreSQL
  driver, Validation, Lombok, Flyway
- `application.yml` configured to connect to a local Postgres (via env vars,
  not hardcoded credentials)
- One placeholder `GET /health` endpoint returning `{"status":"UP"}` — that's
  the only endpoint, nothing else
- An empty `src/main/resources/db/migration/` folder ready for my first
  Flyway migration (don't write any migrations — I'm doing that)
- Standard `service` / `controller` / `repository` / `dto` / `entity` package
  structure with no classes in them yet beyond what's needed for `/health`
- A test folder set up with one passing smoke test (e.g. context loads) —
  no other tests, no Testcontainers config, I'm adding that myself

### 2. application-service (Spring Boot)
- Identical setup to job-service (same dependency set, same package
  structure, own `application.yml` pointing at its own database/schema), also
  with just a `/health` endpoint and nothing else

### 3. frontend (React + Vite)
- Vite + React scaffold, Tailwind installed and configured
- React Router installed with three route placeholders: `/apply`,
  `/admin/jobs`, `/admin/applications` — each rendering a `<div>` with just
  the page name, no real components yet
- Basic `src/` structure: `pages/`, `components/`, `api/` (empty folders
  except the three page files)
- A simple `api/client.js` with a bare axios or fetch instance pointed at
  placeholder base URLs for both services (as env vars) — no actual API
  calls written

### 4. docker-compose.yml
- One Postgres container, with two databases/schemas created on init
  (`job_service`, `application_service`) via an init script
- job-service and application-service NOT containerized yet — I'll run those
  from my IDE while building; just document how in the README
- Frontend NOT containerized — I'll run it with `npm run dev`

### 5. README.md
- How to start Postgres (`docker compose up`)
- How to run each Spring Boot service from the IDE/CLI, and which port each
  listens on
- How to run the frontend (`npm install && npm run dev`) and its port
- A short "what's here vs what's not built yet" section so it's clear this
  is a skeleton, not a finished slice

## How I want you to work

- Scaffold only — don't add any entities, endpoints, business logic, or
  tests beyond exactly what's listed above, even if it seems like an obvious
  next step. I'm intentionally building those myself.
- After scaffolding, tell me how to verify each piece works (start Postgres,
  hit `/health` on both services, load each frontend route) so I know the
  skeleton is solid before I start building on it.
- Keep dependency versions current and compatible with each other — check
  versions rather than assuming from memory.
- If you're unsure whether something counts as "skeleton" vs "logic" (e.g.
  should the axios instance have interceptors?), default to leaving it out
  and ask me.

## Explicitly out of scope

Any entity beyond nothing, any CRUD endpoint, any React component with real
state or data-fetching, any Flyway migration content, any tests beyond one
smoke test per service, Kafka, auth, API gateway, Docker for the app
services themselves. All of this comes from me doing the activity plan, or
from later Claude Code prompts once I've built the fundamentals.
