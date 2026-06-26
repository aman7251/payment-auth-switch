# Payment Authorization Switch

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A real-time card **authorization switch** in Java 17 / Spring Boot. It receives a transaction,
validates the card, checks limits and balance, routes to a (mock) issuer, and returns an
**approve/decline** decision — the core authorization loop of a card switch.

It speaks two protocols into **one** engine:
- **ISO 8583 over TCP** (via [jPOS](https://jpos.org)) — the real switch interface (`0100` → `0110`).
- **REST `/authorize`** (JSON + Swagger UI) — easy to demo and test.

> Resume line it earns: *"Built a real-time card authorization switch in Java/Spring Boot that
> parses ISO 8583 messages, performs limit/balance checks, and returns sub-100ms approve/decline
> responses, backed by PostgreSQL."*

## Architecture

```
  Acquirer sim ──TCP(ISO 8583)──► jPOS ISOServer ─┐
                                                   ├─► AuthorizationService ─► PostgreSQL
  curl / Swagger ──HTTP(JSON)───► REST controller ─┘     (card, limits, balance, issuer)
```

Decision rules (first failure wins), returned as an ISO 8583 response code:

| Code | Meaning | Trigger |
|------|---------|---------|
| `00` | Approved | all checks pass |
| `14` | Invalid card | PAN not found |
| `05` | Do not honor | card blocked |
| `54` | Expired card | past expiry |
| `91` | Issuer unavailable | mock issuer down (BIN 9999) |
| `61` | Exceeds limit | over per-txn or daily limit |
| `51` | Insufficient funds | balance < amount |

## Stack
Java 17 · Spring Boot 3 · jPOS (ISO 8583) · Spring Data JPA · PostgreSQL · Flyway · Docker · springdoc/Swagger · Micrometer/Prometheus.

## Quickstart (Docker — easiest)
```bash
docker compose up --build
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- ISO 8583 TCP switch: localhost:10000

Try the REST API:
```bash
curl -s localhost:8080/authorize -H "Content-Type: application/json" -d '{
  "pan":"4111111111111111","expiry":"3012","amount":1000,
  "currency":"840","stan":"000001","rrn":"000000000001"}'
```

Try the ISO 8583 switch (after the stack is up):
```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.authswitch.tools.AcquirerSimulator
```

## Demo cards (seeded)
| PAN | Result |
|-----|--------|
| 4111111111111111 | Approved (00) |
| 5555555555554444 | Insufficient funds (51) |
| 4000000000000002 | Do not honor — blocked (05) |
| 4000000000000010 | Expired card (54) |
| 9999000000000004 | Issuer unavailable (91) |

## Run locally without Docker
Start a PostgreSQL, then:
```bash
mvn spring-boot:run
```
(Defaults connect to `localhost:5432/authswitch`, user/pass `authswitch`.)

## Tests
```bash
mvn test
```
Unit tests cover the decision engine and the ISO 8583 mapping. They run offline (no DB, no Docker).

## Docs
- `EXPLAINER.md` — deep walkthrough, written for someone new to Java/Spring Boot.
- `HANDOFF.md` — how to run, test, push, and deploy.
- `DEPLOY.md` — public deployment (Railway / Fly.io / Render + Neon Postgres).

## License
MIT — see `LICENSE`. Uses test PANs only; no real cardholder data.
