# Free Deploy — Oracle Always Free (Frontend + Backend + DB)

Best free always-on host for FleetFlow / Transport ERP using the existing Docker setup.

## What gets hosted

| Service   | Public? | Port |
|-----------|---------|------|
| Frontend (nginx + Angular) | Yes | **80** |
| Backend (Spring Boot) | No (internal only) | 8080 inside Docker |
| PostgreSQL | No (internal only) | 5432 inside Docker |

Browser uses one URL. API calls go to `/api/...` and nginx proxies them to the backend.

## Files

- `docker-compose.prod.yml` — production stack
- `.env.prod.example` — copy to `.env.prod` and fill secrets
- `.env.prod` — **do not commit**

## 1. Create Oracle Always Free VM

1. Sign up: https://www.oracle.com/cloud/free/
2. Create Compute instance:
   - Image: **Ubuntu 22.04**
   - Shape: **VM.Standard.A1.Flex** (Ampere)
   - Suggest: 2 OCPU, 12 GB RAM (within Always Free)
3. Download SSH key / note public IP
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

## 3. Put the project on the VM

Option A — Git (recommended):

```bash
git clone <YOUR_GITHUB_MONOREPO_OR_PARENT_FOLDER_URL> fleetflow
cd fleetflow
# folder must contain: docker-compose.prod.yml, transport-frontend/, transport-backend/
```

Option B — copy from Windows with SCP:

```bash
# from your PC
scp -r "D:\Mohan Programs\transport-frontend" "D:\Mohan Programs\transport-backend" \
  "D:\Mohan Programs\docker-compose.prod.yml" "D:\Mohan Programs\.env.prod.example" \
  ubuntu@YOUR_VM_IP:~/fleetflow/
```

## 4. Configure secrets

```bash
cd ~/fleetflow   # or your path
cp .env.prod.example .env.prod
nano .env.prod
```

Set at least:

- `POSTGRES_PASSWORD` — strong password
- `JWT_SECRET` — long random string (`openssl rand -base64 48`)
- `APP_BOOTSTRAP_ADMIN_PASSWORD` — first admin password if bootstrap=true
- First deploy: `APP_BOOTSTRAP_ENABLED=true`
- After AKS/demo seed loaded: set `false` and recreate backend

## 5. Start production stack

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

Open: `http://YOUR_VM_PUBLIC_IP`

Login (if bootstrap created admin): username `admin` + password from `APP_BOOTSTRAP_ADMIN_PASSWORD`.

## 6. Load AKS business seed (optional)

After Flyway migrations are applied and containers are healthy:

```bash
# copy seed folder to VM, then:
docker compose -f docker-compose.prod.yml exec -T postgres \
  psql -U transport_admin -d transport_erp < database/seed/000_reset_all.sql
# then run 001…050 in order, or use your consolidated seed
```

Then set `APP_BOOTSTRAP_ENABLED=false` in `.env.prod` and:

```bash
docker compose -f docker-compose.prod.yml up -d backend
```

## 7. Useful commands

```bash
# stop
docker compose -f docker-compose.prod.yml down

# stop + delete DB volume (DESTROYS DATA)
docker compose -f docker-compose.prod.yml down -v

# rebuild after code change
docker compose -f docker-compose.prod.yml up -d --build
```

## 8. HTTPS (optional, free)

Put Cloudflare in front of the VM IP, or install Caddy/Certbot for Let's Encrypt on port 443.

## Security checklist

- [ ] `.env.prod` exists and is not in Git
- [ ] Strong DB + JWT secrets
- [ ] Postgres port not public
- [ ] Backend port not public
- [ ] Bootstrap disabled after real seed
- [ ] Change default demo passwords after first login

## Why this is better than localhost DB

Cloud frontend/backend cannot use your laptop’s `localhost` PostgreSQL reliably.  
This compose keeps DB next to the backend on the same free VM — correct and free.
