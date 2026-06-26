# DEPLOY — public hosting

The app has two interfaces: a **REST API** (host this publicly for your resume link) and an
**ISO 8583 TCP** server (needs a platform that allows raw TCP; optional for the public demo).

## Database first (free, always-on)
Create a Postgres on **Neon** (https://neon.tech) or **Supabase** (https://supabase.com). You'll get
a host, database, user, and password. Build a JDBC URL:
```
jdbc:postgresql://<host>/<db>?sslmode=require
```

## Option A — Railway (easiest; HTTP + a TCP port)
1. Push the repo to GitHub.
2. railway.app → **New Project → Deploy from GitHub repo** (auto-detects the `Dockerfile`).
3. **Variables**:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<host>/<db>?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `<user>`
   - `SPRING_DATASOURCE_PASSWORD` = `<password>`
   - (optional) `ISO_SERVER_ENABLED=true`, `ISO_SERVER_PORT=10000`
4. Deploy → public URL like `https://payment-auth-switch.up.railway.app`.
5. Demo link: `https://.../swagger-ui.html`. To expose ISO, add a Railway **TCP Proxy** to port 10000.

## Option B — Fly.io (best for the real ISO/TCP demo)
```bash
fly launch         # detects Dockerfile + fly.toml
fly secrets set SPRING_DATASOURCE_URL="jdbc:postgresql://<host>/<db>?sslmode=require" \
                SPRING_DATASOURCE_USERNAME="<user>" \
                SPRING_DATASOURCE_PASSWORD="<password>"
fly deploy
```
`fly.toml` already declares an HTTP service (REST) and a raw TCP service (ISO on 10000), so an
external acquirer simulator can connect over the internet:
```bash
java -cp target/classes com.example.authswitch.tools.AcquirerSimulator <app>.fly.dev 10000
```

## Option C — Render (REST only, $0, sleeps when idle)
New **Web Service** from the repo (Docker). Set the same `SPRING_DATASOURCE_*` env vars. Render
exposes HTTP only, so the ISO/TCP server isn't reachable externally there — fine for a REST demo.

## After deploy — verify
```bash
curl -s https://<your-app>/authorize -H "Content-Type: application/json" -d '{
  "pan":"4111111111111111","expiry":"3012","amount":1000,
  "currency":"840","stan":"000001","rrn":"000000000001"}'
```

## Resume links
```
Payment Authorization Switch — real-time ISO 8583 card auth (Java/Spring Boot)
Live API/Swagger: https://<your-app>/swagger-ui.html
Code:             https://github.com/<you>/payment-auth-switch
```

## Notes
- The app binds to `$PORT` (cloud) or 8080 (local) automatically.
- Flyway creates the schema and seeds demo cards on first boot against the cloud DB.
- Keep DB credentials in the platform's **secrets/variables**, never in the repo.
