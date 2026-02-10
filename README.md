# ArchiGenTN

**Generateur de plans architecturaux alimente par l'IA pour le marche tunisien.**

ArchiGenTN est une plateforme qui permet aux architectes, ingenieurs et particuliers de generer automatiquement des plans architecturaux adaptes aux normes et contraintes du marche tunisien (reglementations urbaines, climat mediterraneen, materiaux locaux).

## Features principales

- **Generation de plans IA** : Generation automatique de plans architecturaux a partir de parametres utilisateur (surface, nombre de pieces, style, budget)
- **Export DXF/AutoCAD** : Export des plans en format DXF compatible avec AutoCAD et autres logiciels CAO
- **Visualisation 3D** : Rendu 3D des plans generes via Blender headless
- **Analyse structurelle** : Verification de la faisabilite structurelle avec PyNite
- **Normes tunisiennes** : Respect des reglementations d'urbanisme tunisiennes (COS, CUF, hauteurs, retraits)
- **Multi-projets** : Gestion de plusieurs projets par utilisateur

## Tech Stack

| Composant | Technologie |
|-----------|------------|
| Frontend | Next.js 16, React 19, Tailwind CSS 4, TypeScript |
| Backend | Spring Boot 3.5, Java 21, jOOQ, Flyway |
| Base de donnees | PostgreSQL 16 |
| Cache | Redis 7 |
| Scripts IA/CAO | Python (ezdxf, Blender, PyNite, NumPy) |
| CI/CD | GitHub Actions |
| Conteneurs | Docker, Docker Compose |

## Quick Start

### Pre-requis

- Docker & Docker Compose
- Java 21+ (pour dev backend)
- Node.js 20+ (pour dev frontend)
- Python 3.11+ (pour scripts)

### Demarrage rapide avec Docker

```bash
# Cloner le repo
git clone https://github.com/votre-org/archigentn.git
cd archigentn

# Lancer tous les services
docker-compose up -d

# Frontend : http://localhost:3000
# Backend  : http://localhost:8080
# Postgres : localhost:5432
```

### Developpement local

```bash
# Backend (Spring Boot)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (Next.js)
cd archigen
npm install
npm run dev

# Scripts Python
cd scripts
python -m venv .venv
source .venv/bin/activate  # ou .venv\Scripts\activate sur Windows
pip install -r requirements.txt
```

## Structure du projet

```
archigentn/
├── backend/              # API Spring Boot (Java 21)
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── archigen/             # Application Next.js (frontend)
│   ├── app/
│   ├── package.json
│   └── Dockerfile
├── scripts/              # Scripts Python (DXF, Blender, PyNite)
│   ├── dxf/              # Generation de fichiers DXF
│   ├── blender/          # Rendu 3D headless
│   ├── structure/        # Analyse structurelle
│   ├── utils/            # Utilitaires geometriques
│   └── requirements.txt
├── docs/                 # Documentation
├── .github/workflows/    # CI/CD GitHub Actions
├── docker-compose.yml    # Environnement de dev
└── README.md
```

## Contribution

1. Fork le projet
2. Creer une branche feature (`git checkout -b feature/ma-feature`)
3. Commiter les changements (`git commit -m 'feat: ajouter ma feature'`)
4. Pousser la branche (`git push origin feature/ma-feature`)
5. Ouvrir une Pull Request

### Conventions de commit

Nous suivons [Conventional Commits](https://www.conventionalcommits.org/) :
- `feat:` nouvelle fonctionnalite
- `fix:` correction de bug
- `docs:` documentation
- `refactor:` refactoring
- `test:` ajout/modification de tests
- `chore:` maintenance

## License

MIT License - voir [LICENSE](LICENSE) pour plus de details.
