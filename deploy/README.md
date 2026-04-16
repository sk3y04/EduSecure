# EduSecure Deployment Templates

This directory contains **template deployment artefacts** that accompany the deployment-stage documentation in `docs/06-operations/production-deployment-runbook.md`.

These files are intentionally marked as examples/templates. They document the **target production-style deployment procedure** for EduSecure, but they are not by themselves proof that a live deployment has already been completed.

## Layout

```text
deploy/
├── README.md
├── home-server/
│   ├── compose.prod.yaml
│   └── .env.prod.example
└── vps/
    └── nginx/
        └── edusecure.conf.example
```

## Purpose of each file

### `home-server/compose.prod.yaml`

Production-style Docker Compose template for the **home server**.

It is designed to:

- run pre-built backend and frontend images
- keep PostgreSQL and MongoDB off the public internet
- run the backend with `SPRING_PROFILES_ACTIVE=prod`
- externalise secrets via an environment file
- persist PostgreSQL, MongoDB, and submission storage on the host
- expose only the frontend (`3000`) and backend (`8080`) ports that the VPS reverse proxy needs over the VPN tunnel

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
| `deploy/home-server/compose.prod.yaml` | home server | `/srv/edusecure/compose.prod.yaml` |
| `deploy/home-server/.env.prod.example` | home server | `/srv/edusecure/.env.prod` |
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

- The repository currently includes a **development** root `compose.yaml`, not a production packaging pipeline.
- These templates expect **pre-built images** for the backend and frontend.
- If you want a fully executable deployment path from this repository alone, the next step would be to add backend/frontend container build files and image-publish automation.

## Recommended usage order

1. read `docs/06-operations/production-deployment-runbook.md`
2. copy the home-server templates into `/srv/edusecure/`
3. replace placeholder image tags and secrets
4. ensure the VPN path between the VPS and home server is working
5. deploy the Compose stack on the home server
6. install the VPS Nginx config and obtain the TLS certificate with Certbot
7. verify HTTPS access and collect evidence if you want to claim live deployment in the final report

