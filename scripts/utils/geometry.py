"""
ArchiGenTN - Fonctions Geometriques
=====================================
Utilitaires geometriques pour la generation de plans et l'analyse spatiale.
"""

import math
from typing import Optional

import numpy as np


def create_rectangle_points(
    x: float, y: float, width: float, height: float
) -> list[tuple[float, float]]:
    """
    Cree les points d'un rectangle.

    Args:
        x, y: Coin inferieur gauche
        width, height: Dimensions

    Returns:
        Liste de 4 points [(x1,y1), (x2,y2), (x3,y3), (x4,y4)]
    """
    return [
        (x, y),
        (x + width, y),
        (x + width, y + height),
        (x, y + height),
    ]


def calculate_area(width: float, height: float) -> float:
    """Calcule la surface d'un rectangle (m2)."""
    return width * height


def calculate_polygon_area(points: list[tuple[float, float]]) -> float:
    """
    Calcule l'aire d'un polygone quelconque (formule du lacet / Shoelace).

    Args:
        points: Liste de points [(x1,y1), (x2,y2), ...]

    Returns:
        Aire en m2 (valeur absolue)
    """
    n = len(points)
    if n < 3:
        return 0.0

    area = 0.0
    for i in range(n):
        j = (i + 1) % n
        area += points[i][0] * points[j][1]
        area -= points[j][0] * points[i][1]

    return abs(area) / 2.0


def offset_polygon(
    points: list[tuple[float, float]], offset: float
) -> list[tuple[float, float]]:
    """
    Decale un polygone vers l'interieur (offset negatif) ou l'exterieur (positif).
    Methode simplifiee pour les rectangles et polygones convexes.

    Args:
        points: Points du polygone
        offset: Distance de decalage (positif = exterieur, negatif = interieur)

    Returns:
        Points du polygone decale
    """
    # TODO: Implementer un offset robuste avec Shapely pour polygones complexes
    # Pour l'instant, methode simplifiee pour rectangles
    if len(points) != 4:
        return points

    # Calculer le centre
    cx = sum(p[0] for p in points) / len(points)
    cy = sum(p[1] for p in points) / len(points)

    # Decaler chaque point vers/depuis le centre
    result = []
    for px, py in points:
        dx = px - cx
        dy = py - cy
        dist = math.sqrt(dx**2 + dy**2)
        if dist == 0:
            result.append((px, py))
            continue
        factor = (dist + offset) / dist
        result.append((cx + dx * factor, cy + dy * factor))

    return result


def point_distance(p1: tuple[float, float], p2: tuple[float, float]) -> float:
    """Distance euclidienne entre deux points."""
    return math.sqrt((p2[0] - p1[0])**2 + (p2[1] - p1[1])**2)


def line_intersection(
    p1: tuple[float, float],
    p2: tuple[float, float],
    p3: tuple[float, float],
    p4: tuple[float, float],
) -> Optional[tuple[float, float]]:
    """
    Intersection de deux segments de droite.

    Args:
        p1, p2: Points du premier segment
        p3, p4: Points du second segment

    Returns:
        Point d'intersection ou None
    """
    x1, y1 = p1
    x2, y2 = p2
    x3, y3 = p3
    x4, y4 = p4

    denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if abs(denom) < 1e-10:
        return None  # Lignes paralleles

    t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
    u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom

    if 0 <= t <= 1 and 0 <= u <= 1:
        x = x1 + t * (x2 - x1)
        y = y1 + t * (y2 - y1)
        return (x, y)

    return None


def meters_to_cm(m: float) -> float:
    """Convertit metres en centimetres."""
    return m * 100.0


def cm_to_meters(cm: float) -> float:
    """Convertit centimetres en metres."""
    return cm / 100.0


def calculate_cos(surface_batie: float, surface_terrain: float) -> float:
    """
    Calcule le Coefficient d'Occupation du Sol (COS).
    Norme urbanistique tunisienne.

    Args:
        surface_batie: Surface au sol construite (m2)
        surface_terrain: Surface totale du terrain (m2)

    Returns:
        COS (ratio)
    """
    if surface_terrain <= 0:
        return 0.0
    return surface_batie / surface_terrain


def calculate_cuf(surface_plancher_totale: float, surface_terrain: float) -> float:
    """
    Calcule le Coefficient d'Utilisation Fonciere (CUF).
    CUF = somme des surfaces de plancher / surface du terrain.

    Args:
        surface_plancher_totale: Somme de toutes les surfaces de plancher (m2)
        surface_terrain: Surface totale du terrain (m2)

    Returns:
        CUF (ratio)
    """
    if surface_terrain <= 0:
        return 0.0
    return surface_plancher_totale / surface_terrain


def check_retrait(
    distance: float,
    min_retrait: float = 3.0,
    type_retrait: str = "lateral",
) -> bool:
    """
    Verifie le respect des retraits reglementaires tunisiens.

    Args:
        distance: Distance mesuree (m)
        min_retrait: Retrait minimum reglementaire (m)
        type_retrait: "frontal", "lateral", "arriere"

    Returns:
        True si conforme
    """
    # Retraits standards en Tunisie (variables selon PAU/commune)
    # Frontal: generalement 5m
    # Lateral: generalement 3m (ou H/2)
    # Arriere: generalement 3m
    return distance >= min_retrait
