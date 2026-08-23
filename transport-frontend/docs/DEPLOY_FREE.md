# Free Deploy Guide

Production deploy files live in a dedicated DevOps repo:

**https://github.com/Mohanrajmohanms7/transport-deploy**

That repo contains:

- `docker-compose.prod.yml`
- `.env.prod.example`
- `README.md` (quick start for DevOps)
- `docs/DEPLOY_FREE_ORACLE.md`

### Quick start (on the server)

```bash
mkdir -p ~/fleetflow && cd ~/fleetflow
git clone https://github.com/Mohanrajmohanms7/transport-frontend.git
git clone https://github.com/Mohanrajmohanms7/transport-backend.git
git clone https://github.com/Mohanrajmohanms7/transport-deploy.git
cd transport-deploy
cp .env.prod.example .env.prod
nano .env.prod
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Open `http://YOUR_VM_PUBLIC_IP`
