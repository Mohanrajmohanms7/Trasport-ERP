# Transport ERP — Deploy pack (inside backend repo)

For DevOps. Also mirrored for a dedicated repo `transport-deploy` (create on GitHub if preferred).

## Repos

- Frontend: https://github.com/Mohanrajmohanms7/transport-frontend
- Backend: https://github.com/Mohanrajmohanms7/transport-backend *(this repo — see `/deployment`)*

## Server layout

```text
fleetflow/
├── transport-frontend/
└── transport-backend/
    └── deployment/     ← run docker compose from here
```

```bash
mkdir -p ~/fleetflow && cd ~/fleetflow
git clone https://github.com/Mohanrajmohanms7/transport-frontend.git
git clone https://github.com/Mohanrajmohanms7/transport-backend.git
cd transport-backend/deployment
cp .env.prod.example .env.prod
nano .env.prod
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Full Oracle guide: [docs/DEPLOY_FREE_ORACLE.md](docs/DEPLOY_FREE_ORACLE.md)
