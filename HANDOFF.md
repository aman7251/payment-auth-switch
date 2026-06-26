# HANDOFF — Payment Authorization Switch

Everything needed to run, test, push, and deploy this project. Do real builds/pushes from your
**personal laptop on home Wi-Fi** (not a corporate machine/network).

## 0. Prerequisites
- **JDK 17** (Temurin/Adoptium): https://adoptium.net
- **Maven 3.9+**: https://maven.apache.org (or use the Docker path, which needs no local Maven)
- **Docker Desktop**: https://www.docker.com (for the one-command run)
- Accounts for deploy: GitHub, plus Railway **or** Fly.io, plus Neon (free Postgres)

## 1. Get the code in place
If you received the `ALL_FILES/` bundle, rebuild the tree first:
```bash
python ALL_FILES/REASSEMBLE.py payment-auth-switch
cd payment-auth-switch
```

## 2. Run it locally (Docker — easiest)
```bash
docker compose up --build
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health:     http://localhost:8080/actuator/health
- ISO switch: TCP localhost:10000

Smoke-test the REST API:
```bash
curl -s localhost:8080/authorize -H "Content-Type: application/json" -d '{
  "pan":"4111111111111111","expiry":"3012","amount":1000,
  "currency":"840","stan":"000001","rrn":"000000000001"}'
```
Expected: `"responseCode":"00"` (approved).

Smoke-test the ISO 8583 switch (separate terminal, app already up):
```bash
mvn -q compile exec:java -Dexec.mainClass=com.example.authswitch.tools.AcquirerSimulator
# -> << received 0110 responseCode(39)=00 authCode(38)=...
```

## 3. Run without Docker
Start a Postgres named `authswitch` (user/pass `authswitch`) on 5432, then:
```bash
mvn spring-boot:run
```

## 4. Test
```bash
mvn test           # offline unit tests (decision engine + ISO mapping)
```

## 5. Demo cards (seeded automatically by Flyway)
| PAN | Outcome | Code |
|-----|---------|------|
| 4111111111111111 | Approved | 00 |
| 5555555555554444 | Insufficient funds | 51 |
| 4000000000000002 | Blocked / do not honor | 05 |
| 4000000000000010 | Expired | 54 |
| 9999000000000004 | Issuer unavailable | 91 |
| 4111111111111111 amount > 20000 | Exceeds per-txn limit | 61 |

## 6. Push to GitHub
```bash
git init
git config user.name  "your-name"
git config user.email "you@example.com"
git add .
git commit -m "feat: payment authorization switch (ISO 8583 + Spring Boot)"
git branch -M main
git remote add origin https://github.com/<you>/payment-auth-switch.git
git push -u origin main
# password = a GitHub Personal Access Token (Contents: Read and write), NOT your account password
```

## 7. Deploy publicly
See `DEPLOY.md`. Short version:
1. Create a free Postgres on **Neon**, copy the connection details.
2. Deploy the repo on **Railway** (or Fly.io) — it auto-builds the `Dockerfile`.
3. Set env vars: `SPRING_DATASOURCE_URL` (jdbc:postgresql://…), `SPRING_DATASOURCE_USERNAME`,
   `SPRING_DATASOURCE_PASSWORD`.
4. Public link: `https://<app>/swagger-ui.html`.

## 8. File manifest
```
payment-auth-switch/
├── pom.xml                      Maven build + dependencies
├── Dockerfile                   Multi-stage build (Maven -> JRE)
├── docker-compose.yml           App + PostgreSQL for local run
├── fly.toml                     Fly.io config (HTTP + raw TCP)
├── .gitignore  .dockerignore
├── README.md  EXPLAINER.md  HANDOFF.md  DEPLOY.md  LICENSE
├── load/authorize.js            k6 load test (proves sub-100ms)
├── .github/workflows/ci.yml     CI: mvn verify
└── src/
    ├── main/java/com/example/authswitch/
    │   ├── PaymentAuthSwitchApplication.java   app entry point
    │   ├── api/         AuthorizationController, ApiExceptionHandler, dto/{Request,Response}
    │   ├── domain/      Account, Card, CardLimit, Transaction, CardStatus
    │   ├── repo/        *Repository (Spring Data JPA)
    │   ├── service/     AuthorizationService, ResponseCode
    │   ├── issuer/      MockIssuer
    │   ├── iso/         IsoMessageService, AuthorizationRequestListener, IsoServer
    │   ├── tools/       AcquirerSimulator (ISO test client)
    │   └── util/        PanHasher
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/  V1__schema.sql, V2__seed_data.sql
    └── test/java/com/example/authswitch/
        ├── service/AuthorizationServiceTest.java
        └── iso/IsoMessageServiceTest.java
```

## 9. Troubleshooting
- **`mvn` not found** → use the Docker path (`docker compose up --build`); it builds inside the image.
- **jPOS dependency not resolving** → it's on Maven Central as `org.jpos:jpos`. If `2.1.7` is
  unavailable, bump `<jpos.version>` in `pom.xml` to another 2.1.x.
- **App fails to start: Flyway/DB** → ensure Postgres is reachable and the `SPRING_DATASOURCE_*`
  values are correct; the JDBC URL must start with `jdbc:postgresql://`.
- **Port already in use** → change `8080`/`10000` mappings in `docker-compose.yml`.
