"""
ArchiGenTN - Analyse Structurelle avec PyNite
===============================================
Calcul de faisabilite structurelle des plans generes.
Verifie que les portees, charges et dimensions sont conformes
aux normes tunisiennes (DTU / Eurocode adapte).

Usage:
    python -m structure.pynite_analysis --input plan.json --output rapport.json

Normes de reference:
    - Charges permanentes: DTU P06-001 (adapte Tunisie)
    - Charges exploitation: habitation 1.5 kN/m², bureaux 2.5 kN/m²
    - Beton: fc28 = 25 MPa (standard Tunisie)
    - Acier: fe = 400 MPa (HA400)
"""

import json
import sys
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Optional

# TODO: Decommenter quand PyNite est installe
# from PyNite import FEModel3D

import numpy as np


# --- Constantes structurelles (normes tunisiennes) ---

@dataclass
class MaterialProperties:
    """Proprietes des materiaux courants en Tunisie."""
    name: str
    E: float          # Module d'elasticite (MPa)
    density: float    # Densite (kN/m3)
    fc: float         # Resistance compression (MPa)
    fy: float         # Limite elastique acier (MPa)


BETON_C25 = MaterialProperties(
    name="Beton C25/30",
    E=31000.0,        # MPa
    density=25.0,     # kN/m3
    fc=25.0,          # MPa
    fy=0.0,
)

ACIER_HA400 = MaterialProperties(
    name="Acier HA400",
    E=200000.0,       # MPa
    density=78.5,     # kN/m3
    fc=0.0,
    fy=400.0,         # MPa
)

# Charges standards (kN/m2)
CHARGES = {
    "permanente_plancher": 5.0,     # Plancher BA 20cm + revetement
    "permanente_toiture": 3.5,      # Toiture terrasse accessible
    "exploitation_habitation": 1.5,  # Habitation
    "exploitation_bureaux": 2.5,     # Bureaux
    "exploitation_commerce": 5.0,    # Commerce
    "cloisons": 1.0,                 # Charge cloisons legeres
}


@dataclass
class StructuralResult:
    """Resultat d'analyse structurelle d'un element."""
    element_type: str        # "poutre", "poteau", "dalle"
    element_id: str
    portee: float            # Portee en metres
    moment_max: float        # Moment flechissant max (kN.m)
    effort_tranchant: float  # Effort tranchant max (kN)
    fleche_max: float        # Fleche maximale (mm)
    fleche_admissible: float # Fleche admissible (mm) = L/500
    is_valid: bool           # Conforme ou non
    message: str


def calculate_beam_preliminary(
    portee: float,
    charge_lineaire: float,
    width: float = 0.25,
) -> StructuralResult:
    """
    Pre-dimensionnement d'une poutre BA (methode simplifiee).

    Args:
        portee: Portee de la poutre (m)
        charge_lineaire: Charge lineaire totale (kN/m)
        width: Largeur de la poutre (m)

    Returns:
        Resultat du pre-dimensionnement
    """
    # Hauteur minimale poutre: L/16 (appuis simples) a L/12
    h_min = portee / 16
    h_recommandee = portee / 12

    # Moment flechissant (poutre simplement appuyee)
    # M = q * L^2 / 8
    moment_max = charge_lineaire * portee**2 / 8

    # Effort tranchant
    # V = q * L / 2
    effort_tranchant = charge_lineaire * portee / 2

    # Fleche simplifiee (poutre rectangulaire)
    # f = 5 * q * L^4 / (384 * E * I)
    h = max(h_min, 0.30)  # Hauteur min 30cm
    I = width * h**3 / 12  # Inertie (m4)
    E = BETON_C25.E * 1000  # kN/m2

    fleche_max = (5 * charge_lineaire * portee**4) / (384 * E * I) * 1000  # mm
    fleche_admissible = portee * 1000 / 500  # mm (L/500)

    is_valid = fleche_max <= fleche_admissible and portee <= 8.0

    message = "OK" if is_valid else ""
    if portee > 8.0:
        message = f"Portee {portee}m trop grande (max recommande: 8m pour BA)"
    elif fleche_max > fleche_admissible:
        message = f"Fleche {fleche_max:.1f}mm > admissible {fleche_admissible:.1f}mm. Augmenter section."

    return StructuralResult(
        element_type="poutre",
        element_id=f"P-{portee:.1f}m",
        portee=portee,
        moment_max=round(moment_max, 2),
        effort_tranchant=round(effort_tranchant, 2),
        fleche_max=round(fleche_max, 2),
        fleche_admissible=round(fleche_admissible, 2),
        is_valid=is_valid,
        message=message,
    )


def analyze_plan(plan_data: dict, building_type: str = "habitation") -> dict:
    """
    Analyse structurelle complete d'un plan.

    Args:
        plan_data: Donnees du plan (format JSON ArchiGenTN)
        building_type: Type de batiment ("habitation", "bureaux", "commerce")

    Returns:
        Rapport d'analyse structurelle
    """
    results = []
    charge_exploitation = CHARGES[f"exploitation_{building_type}"]
    charge_totale = (
        CHARGES["permanente_plancher"]
        + charge_exploitation
        + CHARGES["cloisons"]
    )

    # Analyser chaque piece (portee = plus grande dimension)
    for room in plan_data.get("rooms", []):
        portee = max(room["width"], room["height"])
        # Charge lineaire = charge surfacique * largeur tributaire
        largeur_tributaire = min(room["width"], room["height"]) / 2
        charge_lineaire = charge_totale * largeur_tributaire

        result = calculate_beam_preliminary(portee, charge_lineaire)
        result.element_id = f"Poutre-{room['name']}"
        results.append(result)

    # TODO: Analyse avec PyNite pour modele complet
    # model = FEModel3D()
    # ... ajouter noeuds, elements, charges ...
    # model.analyze()

    all_valid = all(r.is_valid for r in results)

    rapport = {
        "project": "ArchiGenTN",
        "building_type": building_type,
        "charge_totale_kn_m2": charge_totale,
        "elements": [asdict(r) for r in results],
        "global_valid": all_valid,
        "summary": "Structure conforme" if all_valid else "Attention: certains elements necessitent revision",
    }

    return rapport


def main():
    """Point d'entree CLI."""
    if len(sys.argv) < 3:
        print("Usage: python -m structure.pynite_analysis --input plan.json --output rapport.json")
        sys.exit(1)

    input_path = sys.argv[sys.argv.index("--input") + 1]
    output_path = sys.argv[sys.argv.index("--output") + 1]

    with open(input_path, "r") as f:
        plan_data = json.load(f)

    building_type = "habitation"
    if "--type" in sys.argv:
        building_type = sys.argv[sys.argv.index("--type") + 1]

    rapport = analyze_plan(plan_data, building_type)

    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    with open(output, "w") as f:
        json.dump(rapport, f, indent=2, ensure_ascii=False)

    print(f"Rapport structurel genere: {output}")
    if not rapport["global_valid"]:
        print("ATTENTION: Certains elements necessitent une revision!")
        for elem in rapport["elements"]:
            if not elem["is_valid"]:
                print(f"  - {elem['element_id']}: {elem['message']}")


if __name__ == "__main__":
    main()
