# Deployment Guide — Payment Authorization Switch

A complete, click-by-click walkthrough to take this project from your laptop to a **public URL** you
can paste in your resume. Written for someone new to Java/Spring — no prior deploy experience assumed.

> Do all builds/pushes from your **personal laptop on home Wi-Fi**, not a corporate machine/network
> (corporate proxies block Maven downloads, GitHub, and Docker pulls).

---

## Contents
1. [How this app is shaped (read this first)](#1-how-this-app-is-shaped)
2. [Prerequisites (install once)](#2-prerequisites)
3. [Step 1 — Run it locally to confirm it works](#3-step-1--run-it-locally)
4. [Step 2 — Put the code on GitHub](#4-step-2--put-the-code-on-github)
5. [Step 3 — Create a free PostgreSQL (Neon)](#5-step-3--create-a-free-postgresql-neon)
6. [Step 4A — Deploy on Railway (recommended)](#6-step-4a--deploy-on-railway-recommended)
7. [Step 4B — Deploy on Fly.io (for the live ISO 8583/TCP demo)](#7-step-4b--deploy-on-flyio)
8. [Step 4C — Deploy on Render (REST-only, simplest free)](#8-step-4c--deploy-on-render)
9. [Step 5 — Verify your live deployment](#9-step-5--verify-your-live-deployment)
10. [Environment variables reference](#10-environment-variables-reference)
11. [Troubleshooting](#11-troubleshooting)
12. [Cost & keeping it alive](#12-cost--keeping-it-alive)
13. [Resume links](#13-resume-links)

---

## 1. How this app is shaped

The app exposes **two interfaces**:

| Interface | Protocol | Port | Public-friendly? |
|-----------|----------|------|------------------|
| REST API + Swagger UI | HTTP/JSON | 8080 (or `$PORT`) | ✅ yes — this is your resume link |
| ISO 8583 switch | raw TCP | 10000 | ⚠️ only on platforms that allow raw TCP (Fly.io) |

It needs a **PostgreSQL** database. On first boot, **Flyway** automatically creates the tables and
seeds the demo cards — you don't run any SQL by hand.

**Recommended setup:** Railway (or Render) for the public REST API + Neon for the database. If you
specifically want to demo the ISO 8583 TCP leg over the internet, use Fly.io.

```
                 ┌─────────────── your cloud platform ───────────────┐
  Internet ────► │  Docker container (this app)                       │
  (browser,      │   • REST API + Swagger on $PORT                    │ ──► Neon PostgreSQL
   curl)         │   • ISO 8583 TCP server on 10000 (Fly only)        │     (managed, free tier)
                 └────────────────────────────────────────────────────┘
```

---

## 2. Prerequisites

Install these once on your personal laptop:

- **Git** — https://git-scm.com
- **Docker Desktop** — https://www.docker.com/products/docker-desktop (used to run locally; the
  cloud builds the image for you)
- A **GitHub** account — https://github.com
- A **Neon** account (free Postgres) — https://neon.tech
- One of: **Railway** (https://railway.app), **Fly.io** (https://fly.io), or **Render**
  (https://render.com)

You do **not** need Java or Maven installed locally — the `Dockerfile` builds the app with Maven
*inside* the image. (If you want to run `mvn test` directly, install JDK 17 + Maven, but it's
optional.)

---

## 3. Step 1 — Run it locally

This proves the project is healthy before you touch the cloud.

```bash
cd payment-auth-switch
docker compose up --build
```

Wait for the log line `Started PaymentAuthSwitchApplication`. Then open:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health  → should show `{"status":"UP"}`

Test an approval from another terminal:
```bash
curl -s localhost:8080/authorize -H "Content-Type: application/json" -d "{\"pan\":\"4111111111111111\",\"expiry\":\"3012\",\"amount\":1000,\"currency\":\"840\",\"stan\":\"000001\",\"rrn\":\"000000000001\"}"
```
Expected: `"responseCode":"00"`, `"approved":true`.

Stop it with `Ctrl+C`, then `docker compose down`.

> If this works, your code is fine and any cloud problem is configuration (usually the database
> URL), not the app.

---

## 4. Step 2 — Put the code on GitHub

From the project root:
```bash
git init
git add .
git commit -m "feat: payment authorization switch (ISO 8583 + Spring Boot)"
git branch -M main
git remote add origin https://github.com/<your-username>/payment-auth-switch.git
git push -u origin main
```

When prompted for a password, use a **GitHub Personal Access Token**, not your account password:
GitHub → Settings → Developer settings → Personal access tokens → **Fine-grained token** →
Repository access: this repo → Permissions: **Contents = Read and write** → Generate → copy and
paste it as the password.

---

## 5. Step 3 — Create a free PostgreSQL (Neon)

1. Sign in at https://neon.tech → **New Project** (pick any name/region).
2. After it's created, open **Dashboard → Connection Details**.
3. Choose **Parameters / “Connection string”**. You'll see something like:
   ```
   Host:     ep-cool-name-123456.us-east-2.aws.neon.tech
   Database: neondb
   User:     neondb_owner
   Password: npg_xxxxxxxxxxxx
   ```
4. Build a **JDBC URL** in this exact shape (note `jdbc:` prefix and `?sslmode=require`):
   ```
   jdbc:postgresql://ep-cool-name-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
   You'll paste this as `SPRING_DATASOURCE_URL` in the next step, and the user/password separately.

> Neon's free tier is always-on enough for a portfolio demo and requires SSL — that's why
> `?sslmode=require` is included.

---

## 6. Step 4A — Deploy on Railway (recommended)

Railway auto-detects the `Dockerfile` and gives you an HTTPS URL.

1. Go to https://railway.app → **New Project** → **Deploy from GitHub repo** → pick
   `payment-auth-switch`. Authorize GitHub access if asked.
2. Railway starts building from the `Dockerfile`. Let the first build run.
3. Open the service → **Variables** tab → add these (click "New Variable" for each):

   | Name | Value |
   |------|-------|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | `<neon-user>` |
   | `SPRING_DATASOURCE_PASSWORD` | `<neon-password>` |

   (Optional: `ISO_SERVER_ENABLED=false` if you only want REST on Railway.)
4. Go to **Settings → Networking → Generate Domain**. You'll get a URL like
   `https://payment-auth-switch-production.up.railway.app`.
5. Railway redeploys automatically. Watch **Deployments → View Logs** until you see
   `Started PaymentAuthSwitchApplication`.

**Your public demo link:** `https://<your-railway-domain>/swagger-ui.html`

> Want the ISO TCP port public on Railway too? Add a **TCP Proxy** (Settings → Networking) pointing
> to container port `10000`. Railway gives you a `host:port` to connect the simulator to. For most
> resumes the REST/Swagger link is enough.

---

## 7. Step 4B — Deploy on Fly.io

Best choice if you want the **ISO 8583 TCP** server reachable over the internet (the `fly.toml` in
this repo already declares both an HTTP service and a raw TCP service on 10000).

1. Install the CLI: https://fly.io/docs/hands-on/install-flyctl/ then `fly auth login`.
2. From the project root:
   ```bash
   fly launch
   ```
   - It detects the `Dockerfile` and `fly.toml`. Accept the app name or pick one.
   - Say **No** if it offers to create a Postgres (we're using Neon). Say **No** to deploying now.
3. Set the database secrets (these become environment variables securely):
   ```bash
   fly secrets set \
     SPRING_DATASOURCE_URL="jdbc:postgresql://<neon-host>/<db>?sslmode=require" \
     SPRING_DATASOURCE_USERNAME="<neon-user>" \
     SPRING_DATASOURCE_PASSWORD="<neon-password>"
   ```
4. Deploy:
   ```bash
   fly deploy
   ```
5. Get your hostname:
   ```bash
   fly status        # shows the app hostname, e.g. payment-auth-switch.fly.dev
   ```

- REST/Swagger: `https://<app>.fly.dev/swagger-ui.html`
- ISO 8583 demo over the internet (run locally, pointing at Fly):
  ```bash
  mvn -q compile exec:java -Dexec.mainClass=com.example.authswitch.tools.AcquirerSimulator \
    -Dexec.args="<app>.fly.dev 10000"
  ```
  Expect: `<< received 0110 responseCode(39)=00 authCode(38)=...`

---

## 8. Step 4C — Deploy on Render

Simplest always-free option, but **HTTP only** (no public ISO/TCP — fine for a REST demo).

1. https://render.com → **New → Web Service** → connect your GitHub repo.
2. Environment: **Docker** (Render reads the `Dockerfile`). Instance type: **Free**.
3. Add the three `SPRING_DATASOURCE_*` environment variables (same values as above). Optionally set
   `ISO_SERVER_ENABLED=false`.
4. **Create Web Service**. Render builds and gives you `https://payment-auth-switch.onrender.com`.

> Note: Render free instances **sleep** after ~15 min idle; the first request after sleeping takes
> ~30–60s to wake. Acceptable for a portfolio link.

---

## 9. Step 5 — Verify your live deployment

Replace `<your-app>` with your real domain.

**Health:**
```bash
curl -s https://<your-app>/actuator/health
# {"status":"UP"}
```

**Approval (00):**
```bash
curl -s https://<your-app>/authorize -H "Content-Type: application/json" -d "{\"pan\":\"4111111111111111\",\"expiry\":\"3012\",\"amount\":1000,\"currency\":\"840\",\"stan\":\"000001\",\"rrn\":\"000000000001\"}"
```

**Decline examples** (change the PAN):
| PAN | Expected `responseCode` |
|-----|-------------------------|
| 5555555555554444 | `51` insufficient funds |
| 4000000000000002 | `05` blocked |
| 4000000000000010 | `54` expired |
| 9999000000000004 | `91` issuer unavailable |

Or just open `https://<your-app>/swagger-ui.html`, expand **POST /authorize**, click **Try it out**,
and send the JSON from the browser.

---

## 10. Environment variables reference

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | ✅ in cloud | `jdbc:postgresql://localhost:5432/authswitch` | JDBC URL of your Postgres (must start with `jdbc:postgresql://`) |
| `SPRING_DATASOURCE_USERNAME` | ✅ in cloud | `authswitch` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | ✅ in cloud | `authswitch` | DB password |
| `PORT` | auto | `8080` | HTTP port; cloud platforms inject this automatically |
| `ISO_SERVER_ENABLED` | optional | `true` | Set `false` to skip the ISO/TCP server (e.g. on Render) |
| `ISO_SERVER_PORT` | optional | `10000` | ISO 8583 TCP port |

Never commit these values. Always set them in the platform's **Variables/Secrets** UI.

---

## 11. Troubleshooting

**Build fails downloading dependencies / jPOS not found.**
jPOS is on Maven Central as `org.jpos:jpos`. If version `2.1.7` can't resolve, edit `pom.xml` →
`<jpos.version>` to another `2.1.x` (e.g. `2.1.8`), commit, push (cloud rebuilds automatically).

**App starts then crashes; logs mention Flyway / connection refused / `UnknownHostException`.**
The database URL/credentials are wrong. Re-check `SPRING_DATASOURCE_URL` begins with
`jdbc:postgresql://`, has `?sslmode=require` for Neon, and the user/password match Neon exactly.

**`relation already exists` or migration checksum errors.**
You changed a `V*.sql` file after it already ran once. For a throwaway demo DB, drop the schema in
Neon (or create a fresh Neon project) and redeploy so Flyway runs cleanly from scratch.

**Health is UP but `/authorize` returns 400.**
That's validation working — your JSON is missing a field or has a bad format (PAN must be 12–19
digits, `expiry` is `YYMM`, `amount` a positive integer in cents). Check the `fields` in the 400 body.

**Port errors locally.**
Something already uses 8080/10000. Change the left side of the mappings in `docker-compose.yml`.

**Render demo is slow on first hit.**
The free instance was asleep; it wakes in ~30–60s. Refresh once.

---

## 12. Cost & keeping it alive

- **Neon** free tier: fine for a demo; suitable always-on for low traffic.
- **Railway**: small monthly credit on the free/hobby plan — plenty for a portfolio app.
- **Render** free web service: sleeps when idle (cold starts), $0.
- **Fly.io**: small free allowance; good for the TCP demo.

For a "always responds instantly" resume link, Railway or Neon-backed Fly is the smoothest. Render
is the easiest $0 if a cold start is acceptable.

---

## 13. Resume links

```
Payment Authorization Switch — real-time ISO 8583 card authorization (Java / Spring Boot)
• Built a Spring Boot switch that parses ISO 8583 (jPOS), runs card/limit/balance checks,
  and returns sub-100ms approve/decline decisions, backed by PostgreSQL.
Live API (Swagger): https://<your-app>/swagger-ui.html
Source:             https://github.com/<your-username>/payment-auth-switch
```
