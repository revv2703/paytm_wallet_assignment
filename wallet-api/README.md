# Paytm Wallet API

A minimal wallet and peer-to-peer transfer service scaffold aligned to the Paytm PML R2 Agentic Exercise.

### All you need
http://localhost:8080/swagger-ui/index.html

## Architecture

The project is organized by responsibility:

- `api` - HTTP controllers and request/response contracts
- `common` - shared validation, exceptions, and utilities
- `config` - Spring configuration and infrastructure wiring
- `core` - persistence entities and repository contracts
- `service` - wallet, transfer, and idempotency orchestration

## Stack
- Java 25
- Spring Boot 4
- Maven
- PostgreSQL
- Redis
- Docker Compose

## Local development

1. Start infrastructure:
   docker compose up -d postgres redis
2. Run the app:
   mvn spring-boot:run
3. Check health:
   curl http://localhost:8080/health

## Docker

- `docker/Dockerfile` contains a multi-stage image for the app
- `docker-compose.yml` runs the application with PostgreSQL and Redis




# TO DO
- [ ] add loggers and proper error handling
- [ ] add integration tests
- [ ] maybe add an immutable ledger
- [ ] handle correlation ids for tracing requests across services
- [ ] create a dashboard to display request rate, latency p99, error rate, plus **domain counters** (transfers created / declined-insufficient-funds / idempotent-replays)
- [ ] add authentication and authorization