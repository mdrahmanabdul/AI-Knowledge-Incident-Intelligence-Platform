# AKIIP

AI Knowledge and Incident Intelligence Platform: a Maven multi-module Spring Boot workspace.

## Requirements

- Java 21
- Maven 3.9+
- Docker Desktop (running): local stack + Testcontainers
- Ollama (native Windows install) with `ollama pull llama3.2:3b`

## Build

```bash
mvn clean verify
```

The root POM manages Spring Boot, Spring AI, and Testcontainers versions. Java services and shared libraries are Maven modules. Contracts, evaluation, MCP servers, web app, deploy assets, and docs hold assets for their respective stacks.

## Local development

### 1. Start the local stack (Day 2)

```powershell
docker compose -f deploy/docker-compose/docker-compose.yml up -d
docker compose -f deploy/docker-compose/docker-compose.yml ps
```

| Service | From your laptop | Login |
|---|---|---|
| PostgreSQL 17 + pgvector | `localhost:5433`, db `akiip` | akiip / akiip |
| Redis | `localhost:6380` | none |
| Redpanda (Kafka API) | `localhost:19092` | none |
| Object storage, S3 API (RustFS) | `http://localhost:9010` | akiip-dev / akiip-dev-secret-change-me |
| Object storage console | `http://localhost:9011` | same |
| Keycloak admin | `http://localhost:8180` | admin / admin |
| Ollama (native, not in compose) | `http://localhost:11434` | none |

Host ports are non-default on purpose so they don't clash with other local projects.
MinIO no longer publishes free Docker images, so the S3-compatible store is RustFS (same S3 API).

### 2. Database schema + tests (Day 3)

```powershell
mvn -pl ingestion-service -am test
```

Testcontainers starts its own throwaway PostgreSQL + pgvector container, runs Flyway, and verifies:
all 9 tables exist, entities save and read back, and pgvector returns the nearest chunk.

```powershell
mvn -pl ingestion-service spring-boot:run
# log should show: Successfully applied 1 migration to schema "public"
docker exec -it akiip-postgres psql -U akiip -d akiip -c "\dt"
```

Rule: never edit `V1__init.sql` after it ran; add `V2__...sql` instead.

### 3. Ask the LLM through Spring AI (Day 2)

```powershell
mvn -pl query-service spring-boot:run
curl.exe "http://localhost:8081/api/v1/dev/ask?q=What%20is%20Apache%20Kafka%3F"
```

### Service ports

| Service | Port |
|---|---|
| query-service | 8081 |
| ingestion-service | 8082 |

### Troubleshooting

| Symptom | Fix |
|---|---|
| `Could not find a valid Docker environment` (tests) | Start Docker Desktop and wait until it is running |
| `Connection refused: localhost:5433` | Start the compose stack; wait for postgres to be healthy |
| Port already in use | Change the left side of `ports:` in the compose file |
| `model "llama3.2:3b" not found` | `ollama pull llama3.2:3b` |
| Flyway `checksum mismatch` | An applied migration was edited. Add a new V2 instead; for a local reset: `docker compose -f deploy/docker-compose/docker-compose.yml down -v` |
| Eclipse shows red imports after pulling changes | Right-click project → Maven → Update Project (Alt+F5) |
