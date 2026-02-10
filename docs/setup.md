# Guide d'Installation - ArchiGenTN

## Pre-requis

| Outil | Version | Lien |
|-------|---------|------|
| Java JDK | 21+ | [Adoptium](https://adoptium.net/) |
| Node.js | 20+ | [nodejs.org](https://nodejs.org/) |
| Python | 3.11+ | [python.org](https://python.org/) |
| PostgreSQL | 16+ | [postgresql.org](https://postgresql.org/) |
| Docker | 24+ | [docker.com](https://docker.com/) |
| Maven | 3.9+ | Inclus via `mvnw` |
| Blender | 3.6+ | [blender.org](https://blender.org/) (optionnel, pour rendu 3D) |

## Installation rapide (Docker)

```bash
# 1. Cloner le repo
git clone https://github.com/votre-org/archigentn.git
cd archigentn

# 2. Copier les variables d'environnement
cp .env.example .env

# 3. Lancer les services
docker-compose up -d

# 4. Verifier que tout tourne
docker-compose ps

# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
# Postgres: localhost:5432
```

## Installation manuelle (developpement)

### 1. Base de donnees PostgreSQL

```bash
# Creer la base de donnees
psql -U postgres
CREATE DATABASE archigentn;
CREATE USER archigen WITH PASSWORD 'archigen_dev_2024';
GRANT ALL PRIVILEGES ON DATABASE archigentn TO archigen;
\q
```

### 2. Backend (Spring Boot)

```bash
cd backend

# Verifier Java 21
java -version

# Lancer en mode dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou sur Windows:
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# L'API demarre sur http://localhost:8080
```

### 3. Frontend (Next.js)

```bash
cd archigen

# Installer les dependances
npm install

# Lancer en mode dev
npm run dev

# L'application demarre sur http://localhost:3000
```

### 4. Scripts Python

```bash
cd scripts

# Creer un environnement virtuel
python -m venv .venv

# Activer (Linux/macOS)
source .venv/bin/activate

# Activer (Windows)
.venv\Scripts\activate

# Installer les dependances
pip install -r requirements.txt

# Tester la generation DXF
python -m dxf.generate_dxf --input ../docs/sample-plan.json --output output/test.dxf
```

### 5. Blender (optionnel)

```bash
# Installer Blender 3.6+
# Le script utilise le Python embarque de Blender

# Test rendu headless
blender --background --python scripts/blender/render_3d.py -- --input plan.json --output render.png
```

## Variables d'environnement

### Backend (.env ou application-dev.yaml)

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/archigentn
SPRING_DATASOURCE_USERNAME=archigen
SPRING_DATASOURCE_PASSWORD=archigen_dev_2024
JWT_SECRET=your-secret-key-change-in-production
AI_API_KEY=your-ai-api-key
```

### Frontend (.env.local)

```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_NAME=ArchiGenTN
```

## Verification de l'installation

```bash
# Backend health check
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:3000

# PostgreSQL
psql -U archigen -d archigentn -c "SELECT 1;"
```

## Problemes courants

### Port deja utilise
```bash
# Trouver le processus sur le port 8080
lsof -i :8080  # Linux/macOS
netstat -ano | findstr :8080  # Windows
```

### Erreur Flyway migration
```bash
# Reparer les migrations
cd backend
./mvnw flyway:repair -Dflyway.url=jdbc:postgresql://localhost:5432/archigentn
```

### Node modules corrompus
```bash
cd archigen
rm -rf node_modules package-lock.json
npm install
```
