# EXPLAINER — Payment Authorization Switch (for someone new to Java & Spring Boot)

This document explains **what** the project is, **why** each piece exists, and the **Java/Spring
concepts** behind it. Read it top to bottom; by the end you'll be able to explain any file in an
interview.

---

## 1. The domain: what is an "authorization switch"?

When you tap a card, a tiny conversation happens in well under a second:

1. The terminal/acquirer sends an **authorization request** to a **switch**.
2. The switch checks: is the card real? not blocked? not expired? within limits? enough money?
3. It (optionally) asks the **issuing bank** ("issuer"), then replies **approve** or **decline**.
4. The terminal prints "Approved" or "Declined".

That request/response is encoded in a 50-year-old but still-everywhere standard called **ISO 8583**.
This project implements that loop: the *authorization leg* of a switch.

Key vocabulary:
- **PAN** — Primary Account Number (the long card number).
- **BIN** — first 6–8 digits of the PAN; identifies the issuer.
- **STAN** — System Trace Audit Number; a per-message sequence number.
- **RRN** — Retrieval Reference Number; a longer reference for the transaction.
- **MTI** — Message Type Indicator; `0100` = authorization request, `0110` = response.
- **DE** — Data Element; ISO 8583's numbered fields (DE2 = PAN, DE4 = amount, DE39 = response code…).
- **Minor units** — money stored as integer cents (1000 = $10.00) to avoid floating-point errors.

---

## 2. The big picture (one engine, two front doors)

```
  Acquirer simulator ──TCP, ISO 8583──► jPOS ISOServer ──┐
                                                          ├──► AuthorizationService ──► PostgreSQL
  curl / Swagger UI ──HTTP, JSON──────► REST controller ──┘
```

Both doors convert their input into the same `AuthorizationRequest` object and call the same
`AuthorizationService`. That's a deliberate design choice: the business rules live in **one** place,
and the protocols (HTTP vs raw TCP) are just adapters around it.

---

## 3. Java & Spring Boot crash course (the concepts this code uses)

**Java packages & classes.** Java code lives in `package`s that mirror folders
(`com.example.authswitch.service` → `src/main/java/com/example/authswitch/service`). Each `.java`
file usually defines one class.

**Spring Boot** is a framework that wires your app together and runs an embedded web server, so you
just write classes and annotate them.

**Annotations** (the `@Something` lines) are metadata Spring reads at startup:
- `@SpringBootApplication` — marks the entry-point class; turns on auto-configuration + scanning.
- `@RestController` — this class handles HTTP requests and returns JSON.
- `@Service` — a class holding business logic (a "service bean").
- `@Component` — any Spring-managed object (`@Service`/`@RestController` are specializations).
- `@Repository`/`JpaRepository` — data-access objects (Spring writes the SQL for you).
- `@Entity` — a class mapped to a database table.
- `@Transactional` — wrap this method in a DB transaction (all-or-nothing).

**Dependency Injection (DI) / "beans".** You don't create objects with `new` for the big pieces.
Spring creates one instance ("bean") of each `@Service`/`@Component` and **injects** it where needed
via the **constructor**. Example:

```java
public AuthorizationController(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService; // Spring passes the bean in
}
```

This makes code testable: in a unit test we pass in *fakes* instead.

**`Optional<T>`** — a box that either holds a value or is empty; avoids null-pointer bugs.
`cardRepository.findByPanHash(hash)` returns `Optional<Card>`; we call `.isEmpty()` / `.orElseThrow()`.

**`enum`** — a fixed set of named constants (`CardStatus.ACTIVE`, `ResponseCode.APPROVED`).

---

## 4. The layers, file by file

### 4.1 Entry point
- `PaymentAuthSwitchApplication.java` — `main()` calls `SpringApplication.run(...)`. That one line
  boots the whole app: starts the web server, connects to the DB, runs migrations, creates beans.

### 4.2 Configuration
- `application.yml` — settings read at startup. Notable: `server.port: ${PORT:8080}` (use the env
  var `PORT` if set, else 8080 — cloud platforms require this), datasource URL/credentials from env,
  and the ISO server port. `ddl-auto: validate` means **Flyway** owns the schema and Hibernate only
  checks it matches.

### 4.3 Domain (the data model) — package `domain`
Plain classes annotated with `@Entity`; each maps to a table.
- `Account` — holds `balance` (cents), `currency`, `status`, and a `@Version` field. `@Version`
  enables **optimistic locking**: if two authorizations touch the same account at once, the second
  commit fails instead of silently corrupting the balance.
- `Card` — `panHash` (we **never** store the raw PAN), `panLast4`, `expiry` (YYMM), `status`
  (`@Enumerated(STRING)` stores the enum name as text), and a `@ManyToOne` link to `Account`.
- `CardLimit` — `perTxnLimit`, `dailyLimit`, and a rolling `dailySpent` + `windowDate` for the
  velocity check.
- `Transaction` — an audit row for every attempt. The `@UniqueConstraint` on `(stan, rrn)` is what
  makes the system **idempotent**.

### 4.4 Repositories — package `repo`
Interfaces extending `JpaRepository<Entity, IdType>`. You declare a method name and Spring Data
**generates the query**:
```java
Optional<Card> findByPanHash(String panHash); // SELECT * FROM card WHERE pan_hash = ?
```
You get `save`, `findById`, `findAll`, etc. for free. No SQL to write.

### 4.5 The engine — `service/AuthorizationService.java`
This is the heart. `authorize(request)`:
1. **Idempotency check** — if a transaction with this `(stan, rrn)` already exists, return the
   original decision (handles retries/duplicate messages safely).
2. **`decide(...)`** runs the rule chain and returns the first failing `ResponseCode`, or `APPROVED`.
3. On approval only: subtract the amount from the balance, add to `dailySpent`, get an auth code
   from the issuer.
4. Always: save a `Transaction` row (with measured latency) and return an `AuthorizationResponse`.

`@Transactional` ensures steps 3–4 are atomic: if anything throws, the balance change rolls back.
Latency is measured with `System.nanoTime()` (a monotonic clock) and reported in milliseconds.

- `ResponseCode.java` — the enum mapping our decisions to ISO 8583 codes + human messages.
- `util/PanHasher.java` — SHA-256 of the PAN, so the DB only ever holds a hash.
- `issuer/MockIssuer.java` — stands in for the issuing bank; "down" for BIN 9999 to demo code 91.

### 4.6 REST adapter — package `api`
- `AuthorizationController.java` — `@PostMapping` on `/authorize`. `@Valid @RequestBody` tells Spring
  to parse the JSON into an `AuthorizationRequest` **and** validate it (the `@NotBlank`/`@Pattern`
  annotations on the DTO). Returns the response object, which Spring serializes back to JSON.
- `dto/AuthorizationRequest.java` / `AuthorizationResponse.java` — **DTO** = Data Transfer Object,
  the shape of the API in/out. Kept separate from `@Entity` classes so the API and DB can evolve
  independently.
- `ApiExceptionHandler.java` — `@RestControllerAdvice` turns validation errors into a tidy `400`
  JSON instead of a stack trace.

### 4.7 ISO 8583 adapter — package `iso`
- `IsoMessageService.java` — converts a jPOS `ISOMsg` ↔ our request/response. `toResponse` clones
  the request, calls `setResponseMTI()` (turns `0100` into `0110`), sets DE39 (response code) and
  DE38 (auth code).
- `AuthorizationRequestListener.java` — implements jPOS's `ISORequestListener`; its `process(...)`
  is called per incoming message. It maps → calls the engine → sends the reply on the same socket.
- `IsoServer.java` — a `@Component` that, on `ApplicationReadyEvent` (fired once the app is fully
  started), opens a jPOS `ISOServer` on a background thread using an `ASCIIChannel` +
  `ISO87APackager`. `@PreDestroy` shuts it down cleanly.
- `tools/AcquirerSimulator.java` — a `main()` you run separately; it connects as a terminal, sends a
  `0100`, and prints the `0110`. This is your live "switch" demo.

**What is jPOS?** The de-facto Java library for ISO 8583. A **packager** (here `ISO87APackager`)
knows each field's position, length, and encoding, so it can **pack** an `ISOMsg` into bytes for the
wire and **unpack** bytes back into fields. A **channel** (`ASCIIChannel`) frames messages on a TCP
socket (length header + ASCII body). `ISOServer` accepts connections and dispatches messages to your
listener.

### 4.8 Database migrations — `resources/db/migration`
**Flyway** runs versioned SQL files in order at startup and records which ran.
- `V1__schema.sql` — creates the tables.
- `V2__seed_data.sql` — inserts the demo accounts/cards (with precomputed PAN hashes) and resets the
  ID sequences. `window_date` is set in the past so the daily counter resets to "today" on first use.

This means a brand-new database is fully set up automatically the first time the app starts.

---

## 5. A request's life (worked example)

`POST /authorize {pan:4111…1111, amount:1000, stan:000001, rrn:000000000001}`

1. Spring parses + validates JSON → `AuthorizationRequest`.
2. `AuthorizationService.authorize`: no prior `(stan,rrn)` → continue.
3. `decide`: hash the PAN → find the card → ACTIVE, not expired, issuer up, amount 1000 ≤ per-txn
   20000, daily 0+1000 ≤ 30000, balance 50000 ≥ 1000 → **APPROVED (00)**.
4. balance → 49000, dailySpent → 1000, issuer returns auth code, save `Transaction`.
5. Response: `{approved:true, responseCode:"00", authCode:"…", latencyMs:…}`.

The ISO path is identical except step 1 is "unpack `0100`" and step 5 is "pack `0110`".

---

## 6. Testing strategy
- `AuthorizationServiceTest` — pure unit test. Uses **Mockito** to fake the repositories (`@Mock`,
  `when(...).thenReturn(...)`) so it runs with no database, exercising every decision branch.
- `IsoMessageServiceTest` — verifies ISO parsing and that the `0110` carries the right DE38/DE39.
- Both run with `mvn test`, offline. (A full DB integration test using **Testcontainers** is a
  natural next step — the dependency is already in `pom.xml`.)

---

## 7. Running & deploying (summary; details in HANDOFF.md / DEPLOY.md)
- **Local:** `docker compose up --build` → Swagger at `:8080`, ISO at `:10000`.
- **Cloud:** build the Docker image, deploy to **Railway** or **Fly.io**, point it at a free
  **Neon/Supabase** Postgres via env vars (`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`). The REST API
  is your public link; Fly.io can also expose the ISO TCP port.

---

## 8. Design decisions & talking points (interview gold)
- **One engine, two adapters** — protocols are thin; rules are centralized and unit-tested.
- **Idempotency via (STAN, RRN)** — payments retry constantly; duplicates must not double-charge.
- **Money as integer minor units** — never use `double` for money.
- **Optimistic locking (`@Version`)** — safe concurrent balance updates without locking rows.
- **No raw PANs** — only SHA-256 + last4; nods to PCI/tokenization without claiming PCI scope.
- **Flyway-owned schema** — reproducible DB, versioned changes, Hibernate only validates.
- **Latency measured & exposed** — Micrometer/Prometheus + recorded per transaction backs the
  "sub-100ms" claim.

## 9. Limitations / honest scope
- Single-message authorization only (no `0400` reversals, `0200` financial, or settlement) — easy
  extensions.
- The "issuer" is mocked (no real network leg).
- The ISO server uses one packager/channel; a real switch supports many acquirer profiles.
- Test PANs only; this is a portfolio project, not a PCI-certified system.
