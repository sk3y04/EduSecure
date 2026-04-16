# EduSecure Deployment Templates

This directory contains **template deployment artefacts** that accompany the deployment-stage documentation in `docs/06-operations/production-deployment-runbook.md`.

These files are intentionally marked as examples/templates. They document the **target production-style deployment procedure** for EduSecure, but they are not by themselves proof that a live deployment has already been completed.

## Layout

```text
deploy/
├── README.md
├── home-server/
│   ├── compose.yaml
│   ├── compose.prod.yaml
│   ├── .env.prod.example
│   └── mongodb-init/
│       └── 01-create-app-user.js
└── vps/
    └── nginx/
        └── edusecure.conf.example
```

## Purpose of each file

### `home-server/compose.yaml`

Default production-style Docker Compose template for the **home server**.

It mirrors `home-server/compose.prod.yaml` so plain `docker compose` commands work when you are inside the deployment directory on the host.

### `home-server/compose.prod.yaml`

Named production-style Docker Compose template for the **home server**.

It is designed to:

- build production backend and frontend images locally from this repository checkout
- keep PostgreSQL and MongoDB off the public internet
- run the backend with `SPRING_PROFILES_ACTIVE=prod`
- externalise secrets via an environment file
- persist PostgreSQL, MongoDB, and submission storage on the host
- expose only the frontend (`3000`) and backend (`8080`) ports that the VPS reverse proxy needs over the VPN tunnel

For PostgreSQL 18, the home-server Compose files mount `/srv/edusecure/data/postgres` as the parent `/var/lib/postgresql` directory and let `PGDATA` point at the version-specific cluster path. If you already have an older host directory layout from `/var/lib/postgresql/data`, back it up and migrate it carefully instead of deleting it blindly.

### `home-server/mongodb-init/01-create-app-user.js`

MongoDB first-run init script that creates the least-privilege application user used by the chat-enabled backend.

### `../scripts/generate-prod-signing-keypair.sh` and `../scripts/generate-prod-signing-keypair.ps1`

Helper scripts that generate the PEM key pair expected by the production backend mount at `/srv/edusecure/crypto/`.

### `../scripts/generate-prod-secrets.sh` and `../scripts/generate-prod-secrets.ps1`

Helper scripts that generate production-ready `.env.prod` secret values, including the 32-byte Base64 AES keys required for MFA secret encryption and submission-storage key protection.

### `home-server/.env.prod.example`

Example production environment file for the home-server Compose stack.

Copy it to a real deployment location such as:

```bash
cp deploy/home-server/.env.prod.example /srv/edusecure/.env.prod
chmod 600 /srv/edusecure/.env.prod
```

Then replace all placeholder values with real secrets.

### `vps/nginx/edusecure.conf.example`

Example Nginx site configuration for the **OVH VPS**.

It follows the same architecture documented in `homelab-blueprint/NETWORK.md` and `homelab-blueprint/guide/NGINX.md`:

- public DNS resolves to the VPS
- the VPS terminates TLS
- the VPS proxies traffic through the VPN tunnel to the home server
- the home router does not need inbound EduSecure port forwarding

## Expected deployment destinations

Suggested target locations when you actually deploy:

| Repository template | Target host | Suggested destination |
|---|---|---|
| `deploy/home-server/compose.yaml` | home server | `/srv/edusecure/compose.yaml` |
| `deploy/home-server/compose.prod.yaml` | home server | `/srv/edusecure/compose.prod.yaml` |
| `deploy/home-server/.env.prod.example` | home server | `/srv/edusecure/.env.prod` |
| `deploy/home-server/mongodb-init/01-create-app-user.js` | home server | `/srv/edusecure/mongodb-init/01-create-app-user.js` |
| `scripts/generate-prod-signing-keypair.sh` | home server | run from the repository checkout to populate `/srv/edusecure/crypto/` |
| `scripts/generate-prod-secrets.sh` | home server | run from the repository checkout to generate valid `.env.prod` secrets |
| `deploy/vps/nginx/edusecure.conf.example` | OVH VPS | `/usr/local/etc/nginx/conf.d/edusecure.conf` |

## Configuration assumptions

These templates assume the deployment model documented in `docs/06-operations/production-deployment-runbook.md`:

- public domain: `https://edusecure.skey.ovh`
- same-origin browser access:
  - frontend: `https://edusecure.skey.ovh`
  - backend API: `https://edusecure.skey.ovh/api`
- home-server VPN IP example: `10.8.0.2`
- VPS VPN IP example: `10.8.0.1`
- frontend exposed on home server port `3000`
- backend exposed on home server port `8080`
- frontend production build should use `VITE_API_BASE_URL=/api`

## Important limitations

- The home-server deployment now depends on the repository checkout because backend and frontend are built locally with Docker.
- If you later want faster rollouts or remote hosts without the full source tree, the next step would be to add image-publish automation and switch the Compose stack back to published registry images.

## Recommended usage order

1. read `docs/06-operations/production-deployment-runbook.md`
2. place the repository checkout on the home server so the Compose build contexts resolve correctly
3. copy or symlink the home-server deployment files into `/srv/edusecure/`
4. replace secrets in `.env.prod`
5. generate valid `.env.prod` secrets, especially the 32-byte Base64 AES keys used by MFA and submission storage
6. generate the signing key pair under `/srv/edusecure/crypto/`
7. ensure the VPN path between the VPS and home server is working
8. deploy the Compose stack on the home server with `docker compose up --build`
9. install the VPS Nginx config and obtain the TLS certificate with Certbot
10. verify HTTPS access and collect evidence if you want to claim live deployment in the final report

