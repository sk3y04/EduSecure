# Production Deployment Runbook

This document defines the **deployment-stage procedure** for EduSecure.

It is the EduSecure-specific adaptation of the hosting and reverse-proxy methodology documented in `homelab-blueprint/`. It should be used as the main deployment write-up for the repository, while the `homelab-blueprint` guides remain the broader infrastructure foundation.

Concrete companion templates for this runbook now live under `deploy/`:

- `deploy/home-server/compose.prod.yaml`
- `deploy/home-server/.env.prod.example`
- `deploy/vps/nginx/edusecure.conf.example`
- `deploy/README.md`

> [!IMPORTANT]
> This runbook describes the **target production deployment method** for EduSecure. It is a deployment procedure and configuration standard, not automatic proof that a live deployment has already been completed. If the final report includes live-deployment claims, support them with screenshots, `docker compose ps` output, certificate checks, reverse-proxy config excerpts, and domain reachability evidence.

## 1. Purpose and scope

The repository root `compose.yaml` is a **development stack**:

- it bind-mounts the source tree
- it runs the backend with `bootRun`
- it runs the frontend with the Vite development server
- it exposes PostgreSQL and MongoDB ports directly to the host
- it includes local-study default credentials and localhost-friendly CORS/cookie fallbacks

That file is appropriate for local development, but it is **not** the production deployment model.

The deployment-stage model documented here replaces it with:

- immutable frontend and backend container images
- externalised production secrets in a protected environment file
- persistent host storage for PostgreSQL, MongoDB, and encrypted submission content
- a reverse-proxy entry point on the OVH VPS
- a VPN tunnel between the OVH VPS and the home server
- no inbound port-forwarding on the home router
- HTTPS termination and certificate lifecycle management on the VPS

## 2. Deployment methodology alignment with `homelab-blueprint`

EduSecure follows the same operational pattern already documented in:

- `homelab-blueprint/NETWORK.md`
- `homelab-blueprint/guide/NGINX.md`
- `homelab-blueprint/guide/HARDENING.md`
- `homelab-blueprint/guide/MONITORING.md`

For EduSecure, that means:

1. the **OVH VPS** is the only public ingress point
2. the **home server** runs the application containers
3. the VPS and home server are connected by a persistent VPN tunnel
4. Nginx on the VPS terminates TLS and proxies traffic over the tunnel
5. the home router does **not** expose EduSecure ports to the internet

This is the same infrastructure philosophy as the homelab blueprint, but adapted to a single application deployment: `https://edusecure.skey.ovh`.

## 3. Target production topology

```text
Internet
   |
   v
edusecure.skey.ovh -> OVH VPS public IP
   |
   v
FreeBSD VPS
- Nginx reverse proxy
- Certbot / Let's Encrypt
- PF firewall / optional Fail2ban
- OpenVPN server endpoint
   |
   | encrypted VPN tunnel
   v
Home server
- Docker Compose stack for EduSecure
- frontend image (static SPA)
- backend image (Spring Boot)
- PostgreSQL
- optional MongoDB for shared space chat
- encrypted/persistent submission storage
```

### Recommended request flow

- browser requests `https://edusecure.skey.ovh/`
- VPS Nginx proxies `/` to the **frontend container** on the home server over the VPN
- VPS Nginx proxies `/api/` to the **backend container** on the home server over the VPN
- PostgreSQL and MongoDB stay internal to the home-server Compose network and are never reverse-proxied publicly

### Why a single-domain deployment is preferable here

For EduSecure, a same-origin deployment is the simplest and safest posture:

- frontend origin: `https://edusecure.skey.ovh`
- backend API origin: `https://edusecure.skey.ovh/api`

This avoids unnecessary cross-site SPA/API complexity and keeps the current cookie model straightforward:

- `AUTH_COOKIE_SECURE=true`
- `AUTH_COOKIE_SAME_SITE=Lax`
- `APP_CORS_ALLOWED_ORIGINS=https://edusecure.skey.ovh`

## 4. Why the VPS + VPN pattern is more secure

Using the VPS as the public entry point and tunnelling traffic back to the home server improves security in several ways.

### 4.1 No public ports on the home router

The home server does not require port-forwarding for EduSecure. The VPN client on the home server initiates an outbound connection to the VPS, so the home IP address is not directly exposed through DNS or inbound NAT rules.

### 4.2 The home IP address stays hidden

All public DNS records resolve to the OVH VPS public IP, not the residential IP. Attackers only see the VPS as the reachable endpoint.

### 4.3 Smaller internet-facing attack surface

Only the VPS needs to expose public services such as:

- `80/tcp` for ACME challenge handling and HTTP->HTTPS redirect
- `443/tcp` for HTTPS
- the VPN server port on the VPS

The application containers, PostgreSQL, and MongoDB remain off the public internet.

### 4.4 Centralised TLS and logging

TLS certificates, Nginx logs, PF rules, and optional Fail2ban controls are concentrated on one hardened ingress host instead of being distributed across the home network.

### 4.5 Easier access control on the home server

Even after the VPN is established, the home server can restrict frontend/backend access so that only the VPS tunnel endpoint can reach the published application ports.

> [!NOTE]
> The service is not literally reached from the VPS **public IP** once the packet arrives on the home server; the direct source visible there is typically the VPS **VPN tunnel IP** (for example `10.8.0.1`). In practice, the security result is that the public internet can only reach EduSecure through the OVH VPS, while the home server only accepts proxy traffic arriving across the VPN.

## 5. Production packaging assumptions

The deployment-stage Compose file should run **pre-built images**, not source bind mounts.

Recommended packaging model:

- backend image: built from the Spring Boot application and tagged immutably
- frontend image: built as a static site image (for example multi-stage Node build -> Nginx image)
- image registry: GHCR or another private/public container registry you control

A practical workflow is:

1. run automated tests
2. build and tag backend/frontend images
3. push the images to the registry
4. update the image tags in the production Compose file on the home server
5. pull and restart the stack with controlled rollout

### Backend image note

The backend can be packaged with Spring Boot's image build support.

### Frontend image note

The frontend should be built into static assets and served by a small web image rather than by `vite dev`.

When the frontend image is built for production, prefer:

```text
VITE_API_BASE_URL=/api
```

That keeps the browser on the same public origin and lets the VPS Nginx layer route API calls to the backend.

## 6. Recommended home-server directory layout

Example host layout:

```text
/srv/edusecure/
├── compose.prod.yaml
├── .env.prod
├── crypto/
│   ├── signing-private.pem
│   └── signing-public.pem
└── data/
    ├── postgres/
    ├── mongodb/
    └── submission-storage/
```

Recommended protections:

- keep `.env.prod` outside Git and set permissions to owner-read only
- keep the PEM keys in `/srv/edusecure/crypto/` with restrictive permissions
- place `data/` on storage that is encrypted at rest where available
- include `/srv/edusecure/` in the host backup plan

Example home-server preparation:

```bash
sudo mkdir -p /srv/edusecure/data/postgres
sudo mkdir -p /srv/edusecure/data/mongodb
sudo mkdir -p /srv/edusecure/data/submission-storage
sudo mkdir -p /srv/edusecure/crypto
sudo touch /srv/edusecure/.env.prod
sudo chmod 600 /srv/edusecure/.env.prod
sudo chmod 700 /srv/edusecure/crypto
```

## 7. Production-grade Docker Compose design

The production Compose stack should differ from the repository-root development file in the following ways:

- use immutable `image:` references
- do not publish PostgreSQL or MongoDB to the public network
- run the backend with `SPRING_PROFILES_ACTIVE=prod`
- externalise all secrets
- mount persistent storage explicitly
- mount external signing keys explicitly
- restart automatically after reboot/failure
- expose only the frontend and backend host ports needed by the VPS tunnel

### 7.1 Example `compose.prod.yaml`

```yaml
services:
  postgres:
    image: postgres:18-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - /srv/edusecure/data/postgres:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    restart: unless-stopped

  mongodb:
    image: mongo:7.0
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGODB_ROOT_USERNAME}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGODB_ROOT_PASSWORD}
      MONGO_INITDB_DATABASE: ${SPRING_DATA_MONGODB_DATABASE}
      MONGODB_APP_USERNAME: ${MONGODB_APP_USERNAME}
      MONGODB_APP_PASSWORD: ${MONGODB_APP_PASSWORD}
    volumes:
      - /srv/edusecure/data/mongodb:/data/db
      - ./mongodb-init/01-create-app-user.js:/docker-entrypoint-initdb.d/01-create-app-user.js:ro
    healthcheck:
      test: ["CMD-SHELL", "mongosh --quiet --username \"$${MONGO_INITDB_ROOT_USERNAME}\" --password \"$${MONGO_INITDB_ROOT_PASSWORD}\" --authenticationDatabase admin --eval \"quit(db.adminCommand({ ping: 1 }).ok ? 0 : 2)\""]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    restart: unless-stopped

  backend:
    image: ghcr.io/sk3y04/edusecure-backend:2026.04.15
    depends_on:
      postgres:
        condition: service_healthy
      mongodb:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: 8080
      SPRING_DOCKER_COMPOSE_ENABLED: "false"
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS}
      AUTH_COOKIE_SECURE: "true"
      AUTH_COOKIE_SAME_SITE: ${AUTH_COOKIE_SAME_SITE}
      AUTH_COOKIE_DOMAIN: ${AUTH_COOKIE_DOMAIN}
      JWT_SECRET: ${JWT_SECRET}
      MFA_SECRET_ENCRYPTION_KEY: ${MFA_SECRET_ENCRYPTION_KEY}
      AUDIT_HMAC_SECRET: ${AUDIT_HMAC_SECRET}
      SUBMISSION_STORAGE_MASTER_KEY: ${SUBMISSION_STORAGE_MASTER_KEY}
      SUBMISSION_STORAGE_MASTER_KEY_VERSION: ${SUBMISSION_STORAGE_MASTER_KEY_VERSION}
      SUBMISSION_STORAGE_BASE_PATH: /var/lib/edusecure/submission-storage
      CRYPTO_SIGNING_PRIVATE_KEY_LOCATION: file:/run/edusecure/crypto/signing-private.pem
      CRYPTO_SIGNING_PUBLIC_KEY_LOCATION: file:/run/edusecure/crypto/signing-public.pem
      APP_CHAT_ENABLED: "true"
      SPRING_DATA_MONGODB_URI: mongodb://${MONGODB_APP_USERNAME}:${MONGODB_APP_PASSWORD}@mongodb:27017/${SPRING_DATA_MONGODB_DATABASE}?authSource=${SPRING_DATA_MONGODB_DATABASE}
      SPRING_DATA_MONGODB_DATABASE: ${SPRING_DATA_MONGODB_DATABASE}
    ports:
      - "8080:8080"
    volumes:
      - /srv/edusecure/data/submission-storage:/var/lib/edusecure/submission-storage
      - /srv/edusecure/crypto:/run/edusecure/crypto:ro
    restart: unless-stopped
    security_opt:
      - no-new-privileges:true

  frontend:
    image: ghcr.io/sk3y04/edusecure-frontend:2026.04.15
    environment:
      NGINX_ENTRYPOINT_QUIET_LOGS: "1"
    depends_on:
      - backend
    ports:
      - "3000:80"
    restart: unless-stopped
    read_only: true
    tmpfs:
      - /var/cache/nginx
      - /var/run
      - /tmp
    security_opt:
      - no-new-privileges:true
```

### 7.2 Why this Compose layout is production-grade relative to the dev stack

- the database services are **not** published directly
- the backend runs the `prod` profile instead of development defaults
- the frontend is a built image, not a Vite dev server
- source code is not bind-mounted into running containers
- secrets are injected from `.env.prod`
- submission storage is persisted outside the container lifecycle
- signing keys are externalised rather than relying on the classpath demo defaults

### 7.3 Host firewall restriction on the home server

The Compose file above publishes only the frontend and backend ports. Those ports should then be restricted at the host firewall so that **only the VPS tunnel endpoint** can reach them.

Conceptually, allow:

- source `10.8.0.1` -> destination `home-server:3000/tcp`
- source `10.8.0.1` -> destination `home-server:8080/tcp`

and deny other inbound access to those ports.

This preserves the homelab-blueprint design goal:

- the public internet reaches the VPS
- the VPS reaches the home server over VPN
- the home server does not expose EduSecure directly to the internet or the broader LAN unless explicitly allowed

### 7.4 Example Rocky Linux firewall restriction

If the home server follows the Rocky Linux host pattern described in `homelab-blueprint/README.md`, an example `firewalld` approach is:

```bash
sudo firewall-cmd --permanent --new-zone=edusecure-vpn
sudo firewall-cmd --permanent --zone=edusecure-vpn --add-source=10.8.0.1/32
sudo firewall-cmd --permanent --zone=edusecure-vpn --add-port=3000/tcp
sudo firewall-cmd --permanent --zone=edusecure-vpn --add-port=8080/tcp
sudo firewall-cmd --reload
```

Keep those ports closed in every other zone unless you intentionally want LAN access as well.

## 8. Secure environment variables

The application already exposes the settings needed for production via environment variables in `backend/src/main/resources/application.properties` and `backend/src/main/resources/application-prod.properties`.

### 8.1 Example `.env.prod`

```dotenv
POSTGRES_DB=edusecure
POSTGRES_USER=edusecure_app
POSTGRES_PASSWORD=REPLACE_WITH_LONG_RANDOM_PASSWORD

MONGODB_ROOT_USERNAME=edusecure_mongo_root
MONGODB_ROOT_PASSWORD=REPLACE_WITH_LONG_RANDOM_HEX_PASSWORD
MONGODB_APP_USERNAME=edusecure_chat_app
MONGODB_APP_PASSWORD=REPLACE_WITH_LONG_RANDOM_HEX_PASSWORD
SPRING_DATA_MONGODB_DATABASE=edusecure

APP_CORS_ALLOWED_ORIGINS=https://edusecure.skey.ovh
AUTH_COOKIE_SAME_SITE=Lax
AUTH_COOKIE_DOMAIN=

JWT_SECRET=REPLACE_WITH_BASE64_SECRET
MFA_SECRET_ENCRYPTION_KEY=REPLACE_WITH_BASE64_SECRET
AUDIT_HMAC_SECRET=REPLACE_WITH_BASE64_SECRET
SUBMISSION_STORAGE_MASTER_KEY=REPLACE_WITH_BASE64_SECRET
SUBMISSION_STORAGE_MASTER_KEY_VERSION=v1
```

In this production layout, shared space chat is deployed with the rest of the stack, so MongoDB credentials and database settings should always be present. Keep `MONGODB_APP_PASSWORD` URL-safe because Compose injects it directly into the backend MongoDB URI.

### 8.2 Minimum secret-handling rules

- do not commit `.env.prod`
- set filesystem permissions to owner-read only
- store a backup copy in a secure password manager or secrets vault
- rotate any secret after suspected disclosure
- never reuse the development fallbacks from `application.properties`

### 8.3 Secret generation examples

Example commands for Linux/FreeBSD hosts:

```bash
openssl rand -hex 24
openssl rand -base64 32
openssl rand -base64 48
```

Suggested usage:

- database passwords: `openssl rand -hex 24`
- `MONGODB_ROOT_PASSWORD`: `openssl rand -hex 24`
- `MONGODB_APP_PASSWORD`: `openssl rand -hex 24`
- `JWT_SECRET`: `openssl rand -base64 48`
- `MFA_SECRET_ENCRYPTION_KEY`: `openssl rand -base64 32`
- `AUDIT_HMAC_SECRET`: `openssl rand -base64 48`
- `SUBMISSION_STORAGE_MASTER_KEY`: `openssl rand -base64 32`

> [!NOTE]
> The exact acceptable lengths depend on the consuming code path. The important deployment rule is to use strong random values and keep them externalised. If you later standardise exact key lengths in code or validation, update this runbook to match that enforcement precisely.

## 9. Deploying on the home server

This section assumes the home server already follows the homelab-blueprint operating model and has Docker/Compose installed.

### 9.1 Upload the deployment files

Place the production files on the home server:

- `/srv/edusecure/compose.yaml`
- `/srv/edusecure/.env.prod`
- `/srv/edusecure/mongodb-init/01-create-app-user.js`
- `/srv/edusecure/crypto/signing-private.pem`
- `/srv/edusecure/crypto/signing-public.pem`

The repository also keeps `deploy/home-server/compose.prod.yaml` as an equivalent named copy, but the default `compose.yaml` is the most convenient choice for plain `docker compose` commands inside `/srv/edusecure`.

### 9.2 Pull the images and start the stack

```bash
cd /srv/edusecure
sudo docker compose --env-file /srv/edusecure/.env.prod pull
sudo docker compose --env-file /srv/edusecure/.env.prod up -d
sudo docker compose --env-file /srv/edusecure/.env.prod ps
```

This starts PostgreSQL, MongoDB, the backend, and the frontend together as the default production stack.

> [!IMPORTANT]
> The MongoDB init script only runs when `/srv/edusecure/data/mongodb` is empty. If you already have a MongoDB data directory from an earlier unauthenticated deployment, either back it up and reinitialise it before first authenticated startup, or create the root/app users manually before enabling this Compose file.

### 9.3 Verify locally on the home server

```bash
sudo docker compose --env-file /srv/edusecure/.env.prod logs --tail=100 backend
sudo docker compose --env-file /srv/edusecure/.env.prod logs --tail=100 frontend
sudo docker compose --env-file /srv/edusecure/.env.prod logs --tail=50 postgres
sudo docker compose --env-file /srv/edusecure/.env.prod logs --tail=50 mongodb
```

Verify that the frontend and backend respond over the VPN-reachable home-server address after the tunnel is up.

## 10. VPN path from home server to OVH VPS

The network flow should follow the same pattern as `homelab-blueprint/NETWORK.md`:

- VPS runs the VPN server endpoint
- home server runs the VPN client endpoint
- the VPN assigns a tunnel subnet such as `10.8.0.0/24`
- VPS tunnel IP example: `10.8.0.1`
- home server tunnel IP example: `10.8.0.2`

### 10.1 EduSecure-specific upstream targets

A simple EduSecure mapping is:

- frontend container exposed on home server: `10.8.0.2:3000`
- backend container exposed on home server: `10.8.0.2:8080`

The VPS then proxies:

- `https://edusecure.skey.ovh/` -> `http://10.8.0.2:3000`
- `https://edusecure.skey.ovh/api/` -> `http://10.8.0.2:8080/api/`

### 10.2 Security explanation for the report

A clean way to describe this later is:

> EduSecure is deployed behind an OVH VPS reverse proxy. Public DNS resolves only to the VPS, while the application itself runs on the home server and is reachable solely across a private VPN tunnel. This removes the need for home-router port forwarding, hides the residential IP address, concentrates TLS termination and perimeter logging on the VPS, and keeps the backend and databases off the public internet.

## 11. Nginx reverse proxy on the OVH VPS

This section follows the same Nginx methodology as `homelab-blueprint/guide/NGINX.md`, but maps it specifically to EduSecure.

### 11.1 DNS

Create an `A` record:

- `edusecure.skey.ovh` -> `<OVH_VPS_PUBLIC_IP>`

### 11.2 Install Nginx and Certbot on the VPS

On the FreeBSD VPS:

```bash
sudo pkg install nginx py311-certbot
sudo sysrc nginx_enable="YES"
sudo mkdir -p /usr/local/etc/nginx/conf.d
sudo mkdir -p /usr/local/www/.well-known/acme-challenge
sudo mkdir -p /var/log/nginx
```

### 11.3 EduSecure Nginx site configuration

Example `/usr/local/etc/nginx/conf.d/edusecure.conf`:

```nginx
upstream edusecure_frontend {
    server 10.8.0.2:3000;
    keepalive 8;
}

upstream edusecure_backend {
    server 10.8.0.2:8080;
    keepalive 8;
}

server {
    listen 80;
    server_name edusecure.skey.ovh;

    location ^~ /.well-known/acme-challenge/ {
        root /usr/local/www;
        default_type "text/plain";
        allow all;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    http2 on;
    server_name edusecure.skey.ovh;

    ssl_certificate     /usr/local/etc/letsencrypt/live/edusecure.skey.ovh/fullchain.pem;
    ssl_certificate_key /usr/local/etc/letsencrypt/live/edusecure.skey.ovh/privkey.pem;
    ssl_trusted_certificate /usr/local/etc/letsencrypt/live/edusecure.skey.ovh/chain.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_session_tickets off;

    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 1.1.1.1 1.0.0.1 valid=300s;
    resolver_timeout 5s;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 10M;

    location /api/ {
        proxy_pass http://edusecure_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Ssl on;
        proxy_connect_timeout 60s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;
    }

    location / {
        proxy_pass http://edusecure_frontend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_connect_timeout 60s;
        proxy_send_timeout 600s;
        proxy_read_timeout 600s;
    }
}
```

### 11.4 Why the VPS reverse proxy is the right place for the frontend

Placing Nginx on the VPS means:

- the certificate and public HTTPS configuration live on the public ingress host
- the frontend is served behind the same public hostname as the API
- the backend remains hidden behind the tunnel
- the browser gets one clean site entry point: `https://edusecure.skey.ovh`

## 12. Certbot and Let's Encrypt TLS certificate

### 12.1 First certificate issue

Start Nginx with the port-80 server block active, then request the certificate:

```bash
sudo nginx -t
sudo service nginx start
sudo certbot certonly --webroot -w /usr/local/www -d edusecure.skey.ovh
```

After certificate issuance, reload Nginx:

```bash
sudo nginx -t
sudo service nginx reload
```

### 12.2 Automatic renewal

Add a cron entry on the VPS:

```cron
0 3,15 * * * /usr/local/bin/certbot renew --quiet --deploy-hook "service nginx reload"
```

Test renewal safely:

```bash
sudo certbot renew --dry-run
```

### 12.3 What HTTPS protects here

With this setup, Let's Encrypt TLS protects:

- browser -> OVH VPS traffic in transit
- domain authenticity for `edusecure.skey.ovh`
- the public web entry point through certificate validation and HTTPS-only redirection

It does **not by itself** prove:

- encrypted PostgreSQL transport
- encrypted MongoDB transport
- encrypted storage at rest

Those controls must be documented separately where relevant.

## 13. End-to-end deployment sequence

A concise production rollout sequence is:

1. package and publish the backend and frontend images
2. prepare `/srv/edusecure/` on the home server
3. generate and store production secrets in `.env.prod`
4. provision the external signing keypair under `/srv/edusecure/crypto/`
5. bring up the VPN tunnel between the VPS and the home server
6. deploy the Compose stack on the home server
7. verify the frontend and backend are reachable from the VPS across the tunnel
8. configure Nginx on the VPS for `edusecure.skey.ovh`
9. obtain the Let's Encrypt certificate with Certbot
10. reload Nginx and test HTTPS access
11. confirm only the VPS-facing ingress path works as intended
12. collect screenshots/logs/config excerpts if the final report will claim live deployment evidence

## 14. Recommended verification checklist

Use this checklist after deployment:

- [ ] `docker compose ps` shows the expected containers as healthy/running
- [ ] PostgreSQL is not published publicly
- [ ] MongoDB is not published publicly
- [ ] backend runs with `SPRING_PROFILES_ACTIVE=prod`
- [ ] `APP_CORS_ALLOWED_ORIGINS` is set to `https://edusecure.skey.ovh`
- [ ] `AUTH_COOKIE_SECURE=true`
- [ ] `AUTH_COOKIE_SAME_SITE=Lax` for same-origin deployment
- [ ] frontend is reachable from the public domain
- [ ] `/api/` routes successfully through the VPS reverse proxy
- [ ] the certificate is valid for `edusecure.skey.ovh`
- [ ] `certbot renew --dry-run` succeeds
- [ ] the home router has no EduSecure-related inbound port-forwarding
- [ ] the home server only accepts app traffic from the VPS tunnel endpoint
- [ ] backups cover PostgreSQL, MongoDB (if used), `.env.prod`, and signing-key material according to your secure backup policy

## 15. How to reference `homelab-blueprint` in the final report

The cleanest way to cite the relationship is to separate **foundation**, **EduSecure adaptation**, and **deployment evidence**.

### 15.1 Recommended citation model

| Role in report | What to cite | Why |
|---|---|---|
| deployment foundation | `homelab-blueprint/NETWORK.md`, `homelab-blueprint/guide/NGINX.md`, `homelab-blueprint/guide/HARDENING.md` | shows the established hosting methodology and infrastructure standards |
| EduSecure-specific procedure | `docs/06-operations/production-deployment-runbook.md` | shows exactly how that methodology is adapted for EduSecure |
| live evidence, if available | screenshots, `docker compose ps`, `curl -I https://edusecure.skey.ovh`, certbot output, Nginx config excerpt | proves that the documented procedure was actually executed |

### 15.2 Recommended wording for the final report

Suggested wording:

> EduSecure's deployment procedure is based on the same hosting methodology documented in the author's `homelab-blueprint` repository. That blueprint defines the VPN-mediated VPS ingress model, reverse-proxy/TLS workflow, and configuration standards used for self-hosted services. The EduSecure repository then applies that methodology in its own deployment runbook, which maps the shared infrastructure pattern to EduSecure's frontend, backend, database, and domain configuration.

### 15.3 Important honesty boundary

Do **not** present `homelab-blueprint` alone as proof that EduSecure itself is deployed.

Instead:

- cite the blueprint as the **infrastructure foundation**
- cite this runbook as the **EduSecure adaptation**
- cite screenshots/logs/domain checks as the **evidence of actual execution**, if included

That wording keeps the final report technically honest and makes the relationship between the two repositories clear.

## 16. Later report integration

When writing the final report, this document can support:

- the appendix deployment section
- a brief methodology paragraph in the implementation/deployment discussion
- the explanation of HTTPS/TLS as a deployment-side transport control
- the explanation of why the home-server model does not require public exposure of the residential network

For appendix/supporting-material usage, pair this runbook with:

- `docs/06-operations/appendix-cicd-and-deployment-plan.md`
- `docs/06-operations/postgresql-setup-and-security.md`
- selected `homelab-blueprint` infrastructure guides

## 17. Scope reminder

This deployment model is strong for a study project and substantially better than directly exposing a home-hosted stack, but it should still be described honestly.

Safe wording:

- production-style deployment procedure
- hardened self-hosted deployment pattern
- VPS-terminated HTTPS with VPN-backed private upstreams
- databases kept off the public internet

Avoid over-claiming wording such as:

- enterprise-grade zero-trust platform
- fully proven production resilience
- complete operational assurance

Those stronger claims require additional evidence such as monitoring exports, backup restore tests, patch cadence records, incident runbooks, and repeated live verification.


