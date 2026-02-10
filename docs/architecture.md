# Architecture Technique - ArchiGenTN

## Vue d'ensemble

ArchiGenTN suit une architecture **monolithique modulaire** pour le MVP, avec possibilite d'evolution vers des microservices.

```
┌─────────────────────────────────────────────────┐
│                   Client (Browser)               │
│                   Next.js 16 (SSR)               │
└──────────────────────┬──────────────────────────┘
                       │ HTTPS / REST
┌──────────────────────┴──────────────────────────┐
│              API Gateway (Spring Boot)           │
│              - Authentication (JWT)              │
│              - Rate Limiting                     │
│              - CORS                              │
└──┬───────────┬───────────┬──────────────────────┘
   │           │           │
┌──┴──┐   ┌───┴───┐   ┌───┴───┐
│Plans│   │Users  │   │Export │
│Svc  │   │Svc    │   │Svc   │
└──┬──┘   └───┬───┘   └───┬───┘
   │           │           │
┌──┴───────────┴───────────┴──┐
│        PostgreSQL 16         │
│     (+ Redis cache opt.)     │
└─────────────────────────────┘
         │
┌────────┴────────┐
│  Python Scripts  │
│  (DXF, Blender,  │
│   PyNite)        │
└─────────────────┘
```

## Composants

### Frontend (Next.js 16)

- **Framework**: Next.js 16 avec App Router
- **UI**: Tailwind CSS 4 + composants custom
- **State**: React Server Components + client state minimal
- **Auth**: JWT stocke en httpOnly cookie

Pages principales:
- `/` - Landing page
- `/dashboard` - Tableau de bord projets
- `/project/[id]` - Editeur de plan
- `/project/[id]/3d` - Vue 3D
- `/project/[id]/export` - Export DXF/PDF

### Backend (Spring Boot 3.5)

- **Java 21** avec virtual threads
- **jOOQ** pour les requetes SQL type-safe
- **Flyway** pour les migrations DB
- **Spring Security** + JWT

Endpoints principaux:
- `POST /api/plans/generate` - Generer un plan via IA
- `GET /api/plans/{id}` - Recuperer un plan
- `POST /api/plans/{id}/export/dxf` - Exporter en DXF
- `POST /api/plans/{id}/render/3d` - Lancer rendu 3D
- `POST /api/plans/{id}/analyze` - Analyse structurelle

### Scripts Python

Communication backend → Python via:
1. **ProcessBuilder** (Java) pour execution locale
2. **API REST** (Flask/FastAPI) pour mode serveur (futur)

### Base de donnees

PostgreSQL 16 avec schema principal:

```sql
-- Tables principales (gerees par Flyway)
users           -- Utilisateurs
projects        -- Projets architecturaux
plans           -- Plans generes (JSON + metadata)
exports         -- Historique des exports (DXF, PDF, 3D)
terrain_configs -- Configurations terrain (dimensions, reglementations)
```

## Decisions techniques

| Decision | Choix | Justification |
|----------|-------|---------------|
| ORM | jOOQ | Type-safety SQL, performance, controle fin |
| Migrations | Flyway | Standard industrie, versioning SQL |
| Auth | JWT | Stateless, scalable |
| Frontend SSR | Next.js | SEO, performance, DX |
| DXF | ezdxf (Python) | Seule lib mature pour DXF |
| 3D | Blender headless | Gratuit, puissant, scriptable |
| Structure | PyNite | Open source, Python, FEM |

## Flux de donnees

### Generation d'un plan

```
1. User remplit formulaire (surface, pieces, style, terrain)
2. Frontend → POST /api/plans/generate (JSON)
3. Backend valide + appelle IA (TODO: OpenAI/Claude API)
4. IA retourne layout JSON
5. Backend sauvegarde en DB
6. Frontend affiche le plan 2D (canvas/SVG)
```

### Export DXF

```
1. User clique "Exporter DXF"
2. Frontend → POST /api/plans/{id}/export/dxf
3. Backend recupere plan JSON de la DB
4. Backend execute: python -m dxf.generate_dxf --input plan.json --output plan.dxf
5. Backend retourne le fichier DXF en reponse
```

## Securite

- HTTPS obligatoire en production
- JWT avec refresh token
- CORS restreint au domaine frontend
- Rate limiting sur les endpoints de generation
- Validation stricte des inputs (tailles terrain, nombre pieces)
- Sanitization des noms de fichiers (exports)
