# Documentation API - ArchiGenTN

Base URL: `http://localhost:8080/api`

## Authentication

Tous les endpoints (sauf `/auth/*`) necessitent un JWT Bearer token.

```
Authorization: Bearer <token>
```

### POST /auth/register

Inscription d'un nouvel utilisateur.

**Request:**
```json
{
  "email": "architect@example.tn",
  "password": "securePassword123",
  "fullName": "Ahmed Ben Ali",
  "role": "ARCHITECT"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "email": "architect@example.tn",
  "fullName": "Ahmed Ben Ali",
  "role": "ARCHITECT",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### POST /auth/login

Authentification.

**Request:**
```json
{
  "email": "architect@example.tn",
  "password": "securePassword123"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}
```

---

## Plans

### POST /plans/generate

Generer un nouveau plan architectural via IA.

**Request:**
```json
{
  "projectId": 1,
  "terrain": {
    "width": 15.0,
    "height": 25.0,
    "shape": "rectangle"
  },
  "requirements": {
    "totalArea": 120,
    "floors": 1,
    "rooms": [
      {"type": "salon", "minArea": 25},
      {"type": "chambre", "count": 3, "minArea": 12},
      {"type": "cuisine", "minArea": 10},
      {"type": "sdb", "count": 2, "minArea": 5}
    ],
    "style": "moderne",
    "budget": "moyen"
  },
  "regulations": {
    "cos": 0.4,
    "cuf": 1.2,
    "retraitFrontal": 5.0,
    "retraitLateral": 3.0,
    "hauteurMax": 9.0
  }
}
```

**Response:** `201 Created`
```json
{
  "id": 42,
  "projectId": 1,
  "status": "GENERATED",
  "plan": {
    "terrain": {"width": 15, "height": 25},
    "rooms": [
      {"name": "Salon", "x": 0, "y": 0, "width": 5.5, "height": 4.8},
      {"name": "Chambre 1", "x": 5.5, "y": 0, "width": 4.0, "height": 3.5}
    ],
    "doors": [...],
    "windows": [...]
  },
  "metrics": {
    "totalArea": 122.5,
    "cos": 0.33,
    "cuf": 0.33,
    "regulationsCompliant": true
  },
  "createdAt": "2024-01-15T14:20:00Z"
}
```

### GET /plans/{id}

Recuperer un plan par son ID.

**Response:** `200 OK` (meme format que ci-dessus)

### PUT /plans/{id}

Modifier un plan manuellement.

### DELETE /plans/{id}

Supprimer un plan.

---

## Exports

### POST /plans/{id}/export/dxf

Exporter un plan en format DXF (AutoCAD).

**Response:** `200 OK`
- Content-Type: `application/dxf`
- Content-Disposition: `attachment; filename="plan-42.dxf"`

### POST /plans/{id}/render/3d

Lancer un rendu 3D du plan.

**Request:**
```json
{
  "resolution": "1080p",
  "viewType": "aerial",
  "style": "realistic"
}
```

**Response:** `202 Accepted`
```json
{
  "renderId": "render-abc123",
  "status": "PROCESSING",
  "estimatedTime": 30
}
```

### GET /renders/{renderId}

Verifier le statut d'un rendu.

**Response (en cours):** `200 OK`
```json
{
  "renderId": "render-abc123",
  "status": "PROCESSING",
  "progress": 65
}
```

**Response (termine):** `200 OK`
```json
{
  "renderId": "render-abc123",
  "status": "COMPLETED",
  "imageUrl": "/api/renders/render-abc123/image"
}
```

---

## Analyse Structurelle

### POST /plans/{id}/analyze

Lancer une analyse structurelle.

**Response:** `200 OK`
```json
{
  "planId": 42,
  "buildingType": "habitation",
  "chargeTotale": 7.5,
  "elements": [
    {
      "elementType": "poutre",
      "elementId": "Poutre-Salon",
      "portee": 5.5,
      "momentMax": 28.36,
      "effortTranchant": 20.63,
      "flecheMax": 2.1,
      "flecheAdmissible": 11.0,
      "isValid": true,
      "message": "OK"
    }
  ],
  "globalValid": true,
  "summary": "Structure conforme"
}
```

---

## Codes d'erreur

| Code | Description |
|------|-------------|
| 400 | Requete invalide (validation) |
| 401 | Non authentifie |
| 403 | Acces refuse |
| 404 | Ressource non trouvee |
| 409 | Conflit (ex: plan en cours de generation) |
| 422 | Donnees non traitables (ex: terrain trop petit) |
| 429 | Trop de requetes (rate limit) |
| 500 | Erreur serveur |

Format d'erreur standard:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "La surface du terrain doit etre superieure a 50 m2",
  "details": [
    {"field": "terrain.width", "message": "Minimum 5 metres"}
  ]
}
```
