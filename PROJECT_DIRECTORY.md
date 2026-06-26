# Payment Authorization Switch — Directory & Reassembly Guide

A Java 17 / Spring Boot ISO 8583 card authorization switch. This guide lets you (or another AI)
recreate the exact folder structure from the flat `ALL_FILES/` bundle and then build/run it.

## ⚡ TL;DR — reassemble in one command
```bash
python payment-auth-switch/ALL_FILES/REASSEMBLE.py payment-auth-switch
cd payment-auth-switch
docker compose up --build        # Swagger at :8080, ISO 8583 at :10000
```
(Use `python3` on macOS/Linux if needed.)

## The reassembly rule
Inside `ALL_FILES/`, each filename is the file's original path with `/` replaced by `~`:
```
src~main~java~com~example~authswitch~service~AuthorizationService.java
   -> src/main/java/com/example/authswitch/service/AuthorizationService.java
pom.xml -> pom.xml   (no '~' = repo root)
```
`MANIFEST.txt` lists every `flattened_name  ->  original/path`. `REASSEMBLE.py` applies the rule
automatically. The helper files `MANIFEST.txt` and `REASSEMBLE.py` are tools — don't copy them into
the rebuilt project.

## Target structure (after reassembly)
```
payment-auth-switch/
├── pom.xml                      Maven build + dependencies (Spring Boot, jPOS, JPA, Flyway...)
├── Dockerfile                   Multi-stage build (Maven -> JRE)
├── docker-compose.yml           App + PostgreSQL for local run
├── fly.toml                     Fly.io deploy config (HTTP + raw TCP for ISO)
├── .gitignore  .dockerignore
├── README.md                    Overview + quickstart
├── EXPLAINER.md                 Deep walkthrough written for a Java/Spring beginner
├── HANDOFF.md                   Run / test / push / deploy steps + file manifest
├── DEPLOY.md                    Public hosting (Railway / Fly.io / Render + Neon Postgres)
├── LICENSE                      MIT
├── load/authorize.js            k6 load test (asserts p95 < 100ms)
├── .github/workflows/ci.yml     CI: mvn verify on push/PR
└── src/
    ├── main/java/com/example/authswitch/
    │   ├── PaymentAuthSwitchApplication.java   Spring Boot entry point (main)
    │   ├── api/
    │   │   ├── AuthorizationController.java     REST POST /authorize
    │   │   ├── ApiExceptionHandler.java         validation errors -> clean 400 JSON
    │   │   └── dto/
    │   │       ├── AuthorizationRequest.java    request shape + validation
    │   │       └── AuthorizationResponse.java   response shape
    │   ├── domain/
    │   │   ├── Account.java        balance/currency/status (+ @Version optimistic lock)
    │   │   ├── Card.java           pan_hash, last4, expiry, status, account link
    │   │   ├── CardLimit.java      per-txn + daily limits, rolling daily_spent
    │   │   ├── Transaction.java    audit row; unique (stan,rrn) = idempotency
    │   │   └── CardStatus.java     enum ACTIVE/BLOCKED/EXPIRED
    │   ├── repo/
    │   │   ├── AccountRepository.java
    │   │   ├── CardRepository.java          findByPanHash
    │   │   ├── CardLimitRepository.java     findByCardId
    │   │   └── TransactionRepository.java   findByStanAndRrn
    │   ├── service/
    │   │   ├── AuthorizationService.java    THE ENGINE: rules, balance, persistence
    │   │   └── ResponseCode.java            ISO 8583 DE39 codes (00/05/14/51/54/61/91)
    │   ├── issuer/
    │   │   └── MockIssuer.java              stand-in issuer (availability + auth code)
    │   ├── iso/
    │   │   ├── IsoMessageService.java       ISOMsg <-> request/response (0100 -> 0110)
    │   │   ├── AuthorizationRequestListener.java   jPOS per-message handler
    │   │   └── IsoServer.java               starts the jPOS ISO TCP server on boot
    │   ├── tools/
    │   │   └── AcquirerSimulator.java       ISO client: sends 0100, prints 0110
    │   └── util/
    │       └── PanHasher.java               SHA-256 of PAN (never store raw PAN)
    ├── main/resources/
    │   ├── application.yml                  port/$PORT, datasource env, flyway, ISO port
    │   └── db/migration/
    │       ├── V1__schema.sql               tables
    │       └── V2__seed_data.sql            demo accounts/cards (precomputed PAN hashes)
    └── test/java/com/example/authswitch/
        ├── service/AuthorizationServiceTest.java   decision engine (Mockito, offline)
        └── iso/IsoMessageServiceTest.java          ISO parse/build (offline)
```

## After reassembly — build, test, deploy
1. `mvn test` — offline unit tests (no DB/Docker) must pass.
2. `docker compose up --build` — full local run (Postgres + app); Swagger at `/swagger-ui.html`.
3. ISO demo: `mvn -q compile exec:java -Dexec.mainClass=com.example.authswitch.tools.AcquirerSimulator`
4. Deploy: see `DEPLOY.md` (Railway/Fly.io + Neon Postgres); set `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.

## Runtime-only (not in the bundle)
- `target/` (Maven build output), local `.git/`, IDE folders — all regenerated/ignored.

## Notes for the assembler
- Copy files verbatim (UTF-8, don't reformat).
- This is plain Maven; no Maven wrapper is included — use an installed `mvn`, or just use Docker
  (the `Dockerfile` builds with Maven inside the image, so no local Maven is required).
- jPOS resolves from Maven Central as `org.jpos:jpos`; if version `2.1.7` is unavailable, bump
  `<jpos.version>` in `pom.xml` to another `2.1.x`.
