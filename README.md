# ATS Screening Platform

[![CI](https://github.com/prajwalmg/ats-screening-app/actions/workflows/ci.yml/badge.svg)](https://github.com/prajwalmg/ats-screening-app/actions/workflows/ci.yml)

An Applicant Tracking System built as a set of Spring Boot microservices behind
a gateway, with a React frontend and a rule-based automated screening
pipeline. A candidate submits an application with a resume; the system stores
the file, extracts structured data from it, scores it against the job's
requirements, and updates the application's status — all before the
submission's HTTP response comes back.

## Architecture

```
                                ┌─────────────────────┐
                                │       Frontend        │
                                │  React + Vite (nginx) │
                                │       :5173            │
                                └───────────┬─────────┘
                                            │ HTTP (JWT bearer on /api/admin/**)
                                            ▼
                                ┌─────────────────────┐
                                │      API Gateway       │
                                │ Spring Cloud Gateway    │
                                │        :8080            │
                                │                          │
                                │ /api/public/**   → open  │
                                │ /api/admin/**    → JWT   │
                                │ /auth/login      → open  │
                                └───┬──────────┬──────┬───┘
                                    │          │      │
                    ┌───────────────┘          │      └───────────────┐
                    ▼                          ▼                      ▼
          ┌───────────────────┐   ┌─────────────────────┐   ┌───────────────────────┐
          │    job-service      │◄──┤  application-service │   │ resume-storage-service  │
          │       :8081          │   │        :8082          │   │         :8083            │
          │  Postgres (jobs)      │   │  Postgres (applications)│   │   MinIO (resume files)   │
          └───────────────────┘   └──────────┬──────────┘   └───────────┬───────────┘
                    ▲                        │  1. parse                │
                    │                        ▼                          │
                    │           ┌───────────────────────┐                │
                    │           │  resume-parsing-service │◄───────download┘
                    │           │  :8084 — Apache Tika     │
                    │           └────────────┬──────────┘
                    │                        │  2. screen
                    │                        ▼
                    │           ┌───────────────────────┐
                    └───────────┤   ats-screening-service │
              GET requiredSkills│         :8085             │
              & minYearsExperience──────────────────────────┘
```

**Data flow, submit → score:**

1. Frontend uploads the resume file → `POST /api/public/resumes` →
   **resume-storage-service** stores it in MinIO, returns a `resumeUrl`.
2. Frontend submits the application → `POST /api/public/applications` →
   **application-service** confirms the job exists (calls **job-service**),
   saves the application as `SUBMITTED`.
3. **application-service** calls **resume-parsing-service**, which downloads
   the file from MinIO and uses Apache Tika to extract raw text, then
   keyword-matches it against a skill dictionary and regexes out years of
   experience.
4. **application-service** calls **ats-screening-service** with the parsed
   skills/experience and the job id. That service fetches the job's
   `requiredSkills` / `minYearsExperience` from **job-service**, computes a
   match score, applies the experience hard filter, and recommends a status.
5. **application-service** persists the score, matched/missing skills, and
   final status, and returns the whole result in the same response —
   no polling required.

## Tech stack

- **Backend**: Java 17, Spring Boot 4.1, Spring MVC, Spring Data JPA, Flyway,
  PostgreSQL
- **Gateway**: Spring Cloud Gateway (WebMvc/servlet-based), hand-rolled JWT
  auth (`jjwt`)
- **File storage**: MinIO (S3-compatible)
- **Resume parsing**: Apache Tika
- **Testing**: JUnit 5, Mockito, Testcontainers
- **Frontend**: React 19, Vite, React Router, Tailwind CSS, Axios
- **Infra**: Docker Compose

## Services

| Service | Port | Owns | Purpose |
|---|---|---|---|
| `job-service` | 8081 | `job_service` DB | Job postings CRUD |
| `application-service` | 8082 | `application_service` DB | Application intake + orchestrates parse→screen |
| `resume-storage-service` | 8083 | MinIO bucket | Stores resume files, validates type/size |
| `resume-parsing-service` | 8084 | *(stateless)* | Tika text extraction + skill/experience heuristics |
| `ats-screening-service` | 8085 | *(stateless)* | Rule-based scoring against a job's requirements |
| `api-gateway` | 8080 | — | Single entry point, routing, JWT auth on admin routes |
| `frontend` | 5173 | — | React SPA (apply flow + admin console) |

## Running it locally

### Everything, via Docker Compose

Prerequisite: Docker with Compose v2.

```bash
docker compose up -d --build
```

First run builds every service's own image (Maven build inside Docker), so
expect a few minutes. Subsequent runs are fast since layers are cached.

- Frontend: **http://localhost:5173**
- API gateway: **http://localhost:8080**
- MinIO console: **http://localhost:9004** (login `minioadmin` / `minioadmin`)

> **Why MinIO is on 9002/9004, not 9000/9001:** this machine already runs an
> unrelated service on 9000/9001, so those were remapped — the same reasoning
> as Postgres living on `5434` below. Override via `MINIO_API_PORT` /
> `MINIO_CONSOLE_PORT` in a root `.env` if your machine doesn't have that
> conflict.

Default admin login for the frontend's `/admin/*` pages: **admin / admin123**
(override via `ADMIN_USERNAME` / `ADMIN_PASSWORD`).

To stop: `docker compose down` (add `-v` to also drop the Postgres/MinIO
volumes).

### Running a single service against the rest in Docker

Useful when iterating on one service without rebuilding its image each time.
Start everything else via Compose, stop just that one service's container,
then run it from source with the same env vars Compose would have set (see
`docker-compose.yml` for the full list), e.g. for `application-service`:

```bash
docker compose stop application-service
cd application-service
JOB_SERVICE_URL=http://localhost:8081 \
RESUME_PARSING_SERVICE_URL=http://localhost:8084 \
ATS_SCREENING_SERVICE_URL=http://localhost:8085 \
./mvnw spring-boot:run
```

### Running everything from source (no Docker for the apps)

Requires Java 17, Node 20.19+/22.12+, and Docker only for Postgres + MinIO.

```bash
docker compose up -d postgres minio   # infra only
```

Postgres runs on **`5434`** (not `5432` — this machine already has native
Postgres installs on `5432`/`5433`; override `POSTGRES_PORT` in a root `.env`
if yours doesn't conflict). MinIO runs on `9002`/`9004` for the same kind of
reason (see above).

Then, each in its own terminal:

```bash
cd job-service && ./mvnw spring-boot:run                # :8081
cd application-service && ./mvnw spring-boot:run        # :8082
cd resume-storage-service && MINIO_ENDPOINT=http://localhost:9002 ./mvnw spring-boot:run   # :8083
cd resume-parsing-service && ./mvnw spring-boot:run      # :8084
cd ats-screening-service && ./mvnw spring-boot:run       # :8085
cd api-gateway && ./mvnw spring-boot:run                 # :8080
cd frontend && npm install && npm run dev                # :5173
```

All services default their DB/service URLs to `localhost` with the ports
above, so no other env vars are required for local (non-Docker) runs except
`MINIO_ENDPOINT` for `resume-storage-service`, since its Docker default
(`http://localhost:9000`) doesn't match the remapped local port.

## Verification checklist

End-to-end proof that a submission gets stored, parsed, scored, and shows up
correctly in the admin view. Run against Docker Compose (`http://localhost:8080`
for the gateway).

```bash
# 1. Create a job (public endpoint reads jobs, but creation goes through admin login)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/admin/jobs \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Backend Engineer","description":"Java/Spring role","requiredSkills":["Java","Spring Boot","PostgreSQL"],"minYearsExperience":3}'
# → 201, note the returned "id" (assume 1 below)

# 2. Upload a resume (PDF or DOCX, <5MB)
curl -s -X POST http://localhost:8080/api/public/resumes -F "file=@/path/to/resume.pdf"
# → 201 {"resumeUrl": "...", "fileName": "...", "sizeBytes": ...}

# 3. Submit the application using that resumeUrl and job id
curl -s -X POST http://localhost:8080/api/public/applications \
  -H "Content-Type: application/json" \
  -d '{"candidateName":"Jane Doe","email":"jane@example.com","jobId":1,"resumeUrl":"<resumeUrl from step 2>"}'
# → 201, response already includes matchScore / matchedSkills / missingSkills /
#   status / screeningNotes — parsing + screening ran synchronously, no
#   polling needed

# 4. Confirm it shows up in the admin view
curl -s "http://localhost:8080/api/admin/applications?jobId=1" \
  -H "Authorization: Bearer $TOKEN"
# → paginated list including the application from step 3, with the same
#   score/status
```

Or via the UI: open **http://localhost:5173/apply**, pick the job you just
created, fill in the form, attach a resume, submit — the result panel shows
status + score immediately. Then open **/admin/applications**, sign in with
`admin` / `admin123`, and confirm the same application appears with a
matching score, filterable by job and status.

## Deployment

**Platform: Railway.** Chosen over Render or a VPS for this project because
its CLI supports the same kind of browser-authenticated device-code login
already used for GitHub CLI, meaning the deploy itself — not just the choice
— could be driven end-to-end from the terminal. A VPS running the existing
`docker-compose.yml` almost unchanged would need the least new
configuration, but requires manually provisioning a server and handing over
SSH access. Render's free tier doesn't cleanly fit this architecture: no
managed S3-compatible storage, and running 9 resources (6 services + gateway
+ frontend + Postgres) blows past free-tier service-count and memory limits
without cutting real scope (e.g. dropping MinIO/object storage entirely).

**Infrastructure so far:**
- Managed Postgres (`job_service` and `application_service` as two separate
  logical databases on one instance, matching local dev)
- A managed S3-compatible bucket for resumes, used via the same `io.minio`
  SDK client the code already had — no code fork needed for local (MinIO) vs.
  deployed (Railway bucket), since both speak the S3 API. One real
  incompatibility surfaced along the way: Railway's bucket doesn't implement
  the AWS bucket-policy API `resume-storage-service` used to make the bucket
  publicly readable. Fixed by switching to presigned URLs (7-day expiry, the
  SigV4 maximum) instead — arguably a better default anyway (private by
  default, time-limited access) and it works identically against any real
  S3-compatible backend, not just Railway's.
- `job-service`, `application-service`, `resume-storage-service`, and
  `resume-parsing-service` deployed and verified working together over
  Railway's private network (`<service>.railway.internal`) — confirmed with
  a real end-to-end run: create a job, upload a resume, submit an
  application, watch it get parsed and land as `UNDER_REVIEW` with a
  screening-unavailable note (see below for why that note is expected right
  now, and it's a live demonstration of the graceful-degradation design from
  *Judgment calls*, not a bug).

**Status: paused, not yet complete.** `ats-screening-service`, `api-gateway`,
and `frontend` are not deployed — Railway's free "Trial" plan caps the
number of provisioned resources, and the first 6 (Postgres, bucket,
job-service, application-service, resume-storage-service,
resume-parsing-service) already hit that cap. Finishing requires the Hobby
plan (~$5/mo usage-based, needs a payment method — a real cost decision, not
something to make unilaterally). The application-submission pipeline
partially works right now (parsing runs, screening doesn't — it fails
gracefully rather than losing the submission), but there's no public URL
yet since the gateway and frontend, the only two services meant to be
publicly reachable, aren't up.

**To resume:** upgrade the Railway project to the Hobby plan, then repeat
the same recipe used for the four already-deployed services for the
remaining three:
```bash
railway add --service ats-screening-service --json
railway variable set JOB_SERVICE_URL=http://job-service.railway.internal:8081 --service ats-screening-service --skip-deploys
railway variable set OPENAI_API_KEY=<key> --service ats-screening-service --skip-deploys   # only if using the embedding strategy
railway up ./ats-screening-service --path-as-root --service ats-screening-service -c

railway add --service api-gateway --json
# set JOB_SERVICE_URL / APPLICATION_SERVICE_URL / RESUME_STORAGE_SERVICE_URL (all .railway.internal)
# set JWT_SECRET / ADMIN_USERNAME / ADMIN_PASSWORD to real values, not the local demo defaults
# set CORS_ALLOWED_ORIGINS once the frontend's domain is known
railway up ./api-gateway --path-as-root --service api-gateway -c
railway domain --service api-gateway --port 8080   # public URL for the gateway

railway add --service frontend --json
# no runtime env vars — VITE_API_GATEWAY_URL is a *build* arg, baked in at build time
railway up ./frontend --path-as-root --service frontend -c
railway domain --service frontend --port 80        # public URL for the app
```
Then re-run the verification checklist above against the real gateway
domain instead of `localhost:8080`, and re-verify the CORS/JWT preflight
fix specifically (it's a real bug we hit locally — see *Judgment calls* —
and nothing guarantees the deployed environment's CORS behavior matches
local Docker Compose just because the code is identical).

## What this demonstrates

- **Microservice boundaries**: each service owns exactly one concern — job
  postings, application intake, file storage, text extraction, scoring — and
  its own datastore where it needs one. `resume-parsing-service` and
  `ats-screening-service` are intentionally stateless: they're pure request/response
  transformations, which keeps them trivial to test and reason about.
- **Inter-service communication**: synchronous REST throughout, not Kafka.
  `application-service` orchestrates the post-submission pipeline directly —
  see *Judgment calls* below for why, and the trade-off that comes with it.
- **Rule-based "AI screening"**: the scoring is a transparent, explainable
  rule set (keyword-match skill overlap + a hard experience cutoff), not a
  black-box model. For an ATS, where a rejected candidate can reasonably ask
  "why," that explainability is a feature, not a limitation — and the
  screening service's REST interface is exactly where a real ML model would
  slot in later without touching anything else.
- **API gateway + auth**: one enforcement point for JWT auth, with a
  path-based public/admin split (`/api/public/**` vs `/api/admin/**`) so
  individual services stay auth-agnostic.
- **Containerized deployment**: one `docker compose up` builds and starts
  every service, Postgres, and MinIO together.

## Judgment calls

- **REST over Kafka.** The brief allowed either. Kafka would mean a broker,
  topics, and consumer-group plumbing that wouldn't showcase more at this
  scale — three extra services calling each other over HTTP already
  demonstrates inter-service communication. The trade-off: if a downstream
  call is slow or down, the caller notices immediately rather than a message
  sitting durably in a queue for later retry.
- **application-service is the pipeline orchestrator**, not
  `resume-storage-service` chaining forward into parsing into screening.
  Screening needs the job id to look up requirements, and
  `resume-storage-service` has no reason to ever know about jobs — keeping it
  a dumb, reusable file store. `application-service` is the one service with
  full context (candidate, job, resume), so it's the natural place to
  sequence the pipeline.
- **Graceful degradation on pipeline failure.** If `resume-parsing-service`
  or `ats-screening-service` is unreachable or errors, the application is
  *not* rejected or lost — it's saved as `UNDER_REVIEW` with a
  `screeningNotes` explanation, so a human can pick it up. The reasoning:
  the candidate's submission succeeding is the critical path; automated
  scoring is best-effort on top of it.
- **The experience filter is a hard gate, the score isn't.** Below
  `minYearsExperience` auto-rejects regardless of skill match — but the
  match score itself is always pure skill overlap, independent of the gate,
  so an admin reviewing a rejected application can still see how well the
  skills lined up. Above the gate, a configurable threshold (`SCREENING_ADVANCE_THRESHOLD`,
  default 70%) decides auto-advance vs. leaving it in `UNDER_REVIEW` for a
  human.
- **Keyword dictionary, not embeddings/NLP**, for skill *extraction* — a
  fixed list of ~80 common tech terms, matched with word-boundary checks so
  `Java` doesn't false-positive inside `JavaScript`. Deliberately simple and
  explainable; "doesn't need to be perfect" per the brief. (This is separate
  from *scoring*, which is now pluggable — see "Scoring strategies" below.)
- **Denormalized `jobTitle` on `Application`**, captured at submission time
  instead of joining across services for the admin table — a cross-service
  join would break server-side pagination. Trade-off: if a job's title is
  edited later, past applications keep the old title (a snapshot, not a
  live reference).
- **Gateway auth scope**: JWT only gates `/api/admin/**`; `/api/public/**`
  (job browsing, resume upload, application submission) stays open. Admin
  identity is a single hardcoded username/password pair from env vars — no
  user table, no refresh tokens, per "don't over-build this for a resume
  project."
- **Gateway routing as parallel namespaces**: `/api/public/jobs` and
  `/api/admin/jobs` both proxy to the same `job-service` endpoints; the
  gateway's auth filter is then a simple path-prefix check rather than a
  method-aware rule, and the frontend's own public/admin page split maps
  directly onto which prefix it calls.

## Scoring strategies

`ats-screening-service` computes its score via a pluggable `ScoringStrategy`
interface, selected by `screening.strategy` (`SCREENING_STRATEGY` env var),
defaulting to `rule-based` so nothing changes unless you opt in:

```java
public interface ScoringStrategy {
    ScoreResult computeScore(JobDto job, List<String> candidateSkills, String resumeText);
}
```

Two implementations, registered as named Spring beans (the bean name is the
config value):

- **`rule-based`** (default) — the original approach: score = the percentage
  of the job's `requiredSkills` found in the resume's parsed skill list
  (word-boundary keyword match against a fixed dictionary — see
  `resume-parsing-service`'s `SkillDictionary`).
- **`embedding`** — calls OpenAI's `text-embedding-3-small` to embed the
  job's title/description/required-skills text and the resume's extracted
  text (one batched API call, not two), then scores on cosine similarity
  between the two vectors.

Both feed into the **same hard experience gate and the same
matched/missing-skill diagnostic** computed once in `ScreeningService` — the
strategy only changes how the numeric score itself is derived, so a
`REJECTED` for insufficient experience means the same thing regardless of
which strategy is active.

**Why you'd pick one over the other:**

- **Rule-based is interpretable by construction.** The score *is* "3 of 4
  required skills matched" — there's nothing to explain beyond restating the
  computation. For an ATS, where a candidate can reasonably ask why they
  were screened out, that's not a nice-to-have; a keyword-match rejection is
  trivially auditable and defensible. Its ceiling is exactly as literal as
  the word list: a resume that says "distributed event streaming" scores
  zero against a job requiring "Kafka," even though any recruiter would
  recognize the overlap.
- **Embedding-based is semantically flexible but not directly explainable.**
  Cosine similarity between two vectors captures meaning a keyword scan
  can't — synonyms, related technologies, differently-phrased but
  equivalent experience — but "82% similar" isn't a statement you can
  decompose into causes the way "matched 3 of 4 skills" is. You can show
  *what* was compared (the job text, the resume text) but not *which part*
  of either one drove the number.
- **This is a real, general tension in ATS/hiring-tech design**, not
  specific to this project: rule-based systems are auditable but brittle to
  phrasing; embedding/ML-based systems generalize better but are harder to
  justify to a candidate or regulator asking for a reason. Running both
  behind the same interface, on the same inputs, producing directly
  comparable output (see the test below) is a reasonable way to introduce
  the second without committing to it as the only source of truth — an
  admin could plausibly want to see both scores side by side rather than
  trust either alone.

`ScoringStrategyComparisonTest` runs one fixed job/resume pair through both
strategies (the embedding client is mocked with a known vector pair — the
test proves the mechanism is wired correctly, not that OpenAI's embeddings
are good, which isn't something a deterministic unit test should assert).

## Notes on dependency choices

- **Java 17**: the local JDK was 18.0.2.1 (non-LTS); Spring Boot 4.1's
  Initializr offers 17/21/25/26 as targets, and 17 is the newest one that
  bytecode-runs on JRE 18 without a JDK upgrade.
- **react-router-dom 7.18.2**, not `react-router` 8.x: `react-router@8.3.0`
  requires Node ≥22.22, which the local Node (20.20.2) doesn't satisfy.
  `react-router-dom` 7.x is flagged by `npm audit` for a CSRF advisory, but
  that advisory only affects the unstable RSC APIs, which this app (plain
  `BrowserRouter`/`Routes`) doesn't use.
- **Spring Cloud Gateway's WebMvc variant**, not the reactive one: Initializr
  resolved `spring-cloud-starter-gateway-server-webmvc` for this Boot
  version, which keeps the gateway on the same Servlet/Spring MVC stack as
  every other service — no WebFlux/reactive code anywhere in the project.
- **`org.testcontainers:testcontainers-postgresql` /
  `testcontainers-junit-jupiter`**: this Boot/Testcontainers version pair
  renamed the classic `org.testcontainers:postgresql` /
  `org.testcontainers:junit-jupiter` artifact ids with a `testcontainers-`
  prefix.

## Testing

- **Unit tests** (Mockito): service-layer logic in `job-service`,
  `application-service` (including the pipeline's graceful-degradation
  fallback), and `ats-screening-service` (all three status outcomes:
  advanced / rejected / under review).
- **Integration tests** (Testcontainers + MockMvc): one per service with a
  database — `job-service` and `application-service` — running the real
  controller → service → repository → Postgres (with Flyway) stack, with
  sibling-service REST clients mocked at the bean level where a test doesn't
  need them.

Run per service: `./mvnw test` (needs Docker running, for the Testcontainers
tests).
