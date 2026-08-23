# Free Deploy — Oracle Always Free (Frontend + Backend + DB)

For DevOps day-to-day commands, see the root [README.md](../README.md).

## What gets hosted

| Service | Public? | Port |
|---------|---------|------|
| Frontend (nginx + Angular) | Yes | **80** |
| Backend (Spring Boot) | No (internal) | 8080 inside Docker |
| PostgreSQL | No (internal) | 5432 inside Docker |

Browser uses one URL. `/api/...` is proxied by nginx to the backend.

## GitHub repos

- https://github.com/Mohanrajmohanms7/transport-frontend
- https://github.com/Mohanrajmohanms7/transport-backend
- https://github.com/Mohanrajmohanms7/transport-deploy (compose + env example)

## 1. Create Oracle Always Free VM

1. Sign up: https://www.oracle.com/cloud/free/
2. Compute instance:
   - Image: **Ubuntu 22.04**
   - Shape: **VM.Standard.A1.Flex** (Ampere) — e.g. 2 OCPU / 12 GB
3. Note public IP + SSH key
4. VCN Security List ingress:
   - TCP **22** (SSH)
   - TCP **80** (HTTP)
   - TCP **443** (HTTPS later, optional)
   - Do **not** open 5432 or 8080

## 2. Install Docker on the VM

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER
exit   # SSH again so docker group applies
```

## 3. Clone the three repos (siblings)

```bash
mkdir -p ~/fleetflow && cd ~/fleetflow
git clone https://github.com/Mohanrajmohanms7/transport-frontend.git
git clone https://github.com/Mohanrajmohanms7/transport-backend.git
git clone https://github.com/Mohanrajmohanms7/transport-deploy.git
cd transport-deploy
```

Private repos: use a GitHub PAT or SSH deploy key when cloning.

## 4. Configure secrets

```bash
cp .env.prod.example .env.prod
nano .env.prod
```

## 5. Start stack

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml ps
```

Open: `http://YOUR_VM_PUBLIC_IP`

## 6. Optional seed / bootstrap

- First empty DB: `APP_BOOTSTRAP_ENABLED=true` creates admin.
- After loading business SQL seed: set `APP_BOOTSTRAP_ENABLED=false` and recreate backend.

Seed SQL (if used) lives under the backend repo (`database/seed/` or `src/main/resources/db/`).

## 7. HTTPS (optional)

Cloudflare in front of the VM, or Caddy/Certbot on port 443.

## Security checklist

- [ ] `.env.prod` exists and is not in Git
- [ ] Strong DB + JWT secrets
- [ ] Postgres / backend ports not public
- [ ] Bootstrap disabled after real data
- [ ] Change default passwords after first login
