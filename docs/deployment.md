# Guide de Deploiement - ArchiGenTN

## Environnements

| Environnement | URL | Branche |
|---------------|-----|---------|
| Development | localhost | `feature/*` |
| Staging | staging.archigentn.tn | `develop` |
| Production | app.archigentn.tn | `main` |

## Deploiement avec Docker

### Build des images

```bash
# Backend
docker build -t archigentn-backend:latest ./backend

# Frontend
docker build -t archigentn-frontend:latest ./archigen
```

### Docker Compose (Production)

```bash
# Avec variables d'environnement production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Deploiement Cloud

### Option 1: VPS (Recommande pour debut)

Hebergeurs recommandes pour la Tunisie:
- **Hetzner** (EU, bon rapport qualite/prix)
- **OVH** (EU, datacenter proche)
- **DigitalOcean** (US/EU)

Configuration minimum:
- 2 vCPU, 4 GB RAM, 40 GB SSD
- Ubuntu 22.04 LTS

```bash
# Sur le serveur
# 1. Installer Docker
curl -fsSL https://get.docker.com | sh

# 2. Cloner et deployer
git clone https://github.com/votre-org/archigentn.git
cd archigentn
docker-compose -f docker-compose.prod.yml up -d
```

### Option 2: Platform as a Service

- **Backend**: Railway, Render, ou Fly.io
- **Frontend**: Vercel (optimal pour Next.js)
- **Database**: Neon (PostgreSQL serverless)
- **Redis**: Upstash

### Option 3: Kubernetes (futur)

TODO: Configuration Kubernetes pour scaling horizontal.

## Configuration Nginx (Reverse Proxy)

```nginx
# /etc/nginx/sites-available/archigentn
server {
    listen 80;
    server_name app.archigentn.tn;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name app.archigentn.tn;

    ssl_certificate /etc/letsencrypt/live/app.archigentn.tn/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.archigentn.tn/privkey.pem;

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
    }

    # Backend API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## SSL avec Let's Encrypt

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d app.archigentn.tn
```

## Sauvegardes

### PostgreSQL

```bash
# Sauvegarde quotidienne (ajouter en cron)
pg_dump -U archigen -d archigentn -F c -f /backups/archigentn_$(date +%Y%m%d).dump

# Restauration
pg_restore -U archigen -d archigentn /backups/archigentn_20240115.dump
```

### Crontab

```bash
# Sauvegarde DB quotidienne a 2h du matin
0 2 * * * pg_dump -U archigen -d archigentn -F c -f /backups/archigentn_$(date +\%Y\%m\%d).dump

# Nettoyage des sauvegardes > 30 jours
0 3 * * * find /backups -name "archigentn_*.dump" -mtime +30 -delete
```

## Monitoring

### Health Checks

- Backend: `GET /actuator/health`
- Frontend: `GET /api/health` (Next.js API route)
- PostgreSQL: `pg_isready`
- Redis: `redis-cli ping`

### Logs

```bash
# Voir les logs de tous les services
docker-compose logs -f

# Logs d'un service specifique
docker-compose logs -f backend
```

## Checklist pre-deploiement

- [ ] Variables d'environnement configurees
- [ ] Secrets JWT generes (min 256 bits)
- [ ] Base de donnees PostgreSQL provisionnee
- [ ] Migrations Flyway executees
- [ ] CORS configure pour le domaine production
- [ ] HTTPS active (certificat SSL)
- [ ] Sauvegardes automatiques configurees
- [ ] Monitoring/alertes en place
- [ ] Rate limiting active
- [ ] Logs centralises
