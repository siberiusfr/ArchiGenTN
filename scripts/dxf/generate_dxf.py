"""
ArchiGenTN - Generateur de fichiers DXF
========================================
Script principal pour la generation de plans architecturaux au format DXF.
Utilise ezdxf pour creer des fichiers compatibles AutoCAD.

Usage:
    python -m dxf.generate_dxf --input plan.json --output plan.dxf
"""

import json
import sys
from pathlib import Path
from typing import Any

import ezdxf
from ezdxf import units
from ezdxf.document import Drawing

from utils.geometry import (
    calculate_area,
    create_rectangle_points,
    offset_polygon,
)


# Layers standards pour les plans architecturaux tunisiens
LAYERS = {
    "MURS_PORTEURS": {"color": 1, "linetype": "CONTINUOUS"},      # Rouge - murs porteurs
    "MURS_CLOISONS": {"color": 3, "linetype": "CONTINUOUS"},      # Vert - cloisons
    "OUVERTURES": {"color": 5, "linetype": "DASHED"},             # Bleu - portes/fenetres
    "COTATIONS": {"color": 7, "linetype": "CONTINUOUS"},          # Blanc - cotations
    "MOBILIER": {"color": 8, "linetype": "CONTINUOUS"},           # Gris - mobilier
    "TEXTE": {"color": 7, "linetype": "CONTINUOUS"},              # Blanc - annotations
    "TERRAIN": {"color": 30, "linetype": "DASHDOT"},              # Orange - limites terrain
    "RESEAUX": {"color": 6, "linetype": "DASHED"},                # Magenta - reseaux
}


def create_document() -> Drawing:
    """Cree un nouveau document DXF avec les layers standards."""
    doc = ezdxf.new("R2010")
    doc.units = units.M  # Metres (standard tunisien)

    # Creer les layers
    for name, props in LAYERS.items():
        doc.layers.add(name, color=props["color"], linetype=props["linetype"])

    return doc


def draw_room(
    msp: Any,
    x: float,
    y: float,
    width: float,
    height: float,
    name: str,
    wall_thickness: float = 0.20,
) -> None:
    """
    Dessine une piece avec ses murs.

    Args:
        msp: Modelspace du document DXF
        x, y: Coordonnees du coin inferieur gauche
        width, height: Dimensions de la piece (en metres)
        name: Nom de la piece (ex: "Salon", "Chambre 1")
        wall_thickness: Epaisseur des murs (defaut: 20cm standard tunisien)
    """
    # Murs exterieurs
    points = create_rectangle_points(x, y, width, height)
    msp.add_lwpolyline(points, close=True, dxfattribs={"layer": "MURS_PORTEURS"})

    # Murs interieurs (offset)
    inner_points = create_rectangle_points(
        x + wall_thickness,
        y + wall_thickness,
        width - 2 * wall_thickness,
        height - 2 * wall_thickness,
    )
    msp.add_lwpolyline(inner_points, close=True, dxfattribs={"layer": "MURS_CLOISONS"})

    # Label de la piece
    center_x = x + width / 2
    center_y = y + height / 2
    area = calculate_area(width - 2 * wall_thickness, height - 2 * wall_thickness)
    label = f"{name}\n{area:.1f} m²"

    msp.add_mtext(
        label,
        dxfattribs={
            "layer": "TEXTE",
            "insert": (center_x, center_y),
            "char_height": 0.15,
        },
    )


def draw_door(
    msp: Any,
    x: float,
    y: float,
    width: float = 0.90,
    orientation: str = "horizontal",
) -> None:
    """
    Dessine une porte.

    Args:
        msp: Modelspace
        x, y: Position de la porte
        width: Largeur de la porte (defaut: 90cm standard)
        orientation: "horizontal" ou "vertical"
    """
    # TODO: Implementer le dessin de porte avec arc d'ouverture
    # Standard tunisien: porte principale 1.00m, interieure 0.80-0.90m
    if orientation == "horizontal":
        msp.add_line((x, y), (x + width, y), dxfattribs={"layer": "OUVERTURES"})
    else:
        msp.add_line((x, y), (x, y + width), dxfattribs={"layer": "OUVERTURES"})


def draw_window(
    msp: Any,
    x: float,
    y: float,
    width: float = 1.20,
    orientation: str = "horizontal",
) -> None:
    """
    Dessine une fenetre.

    Args:
        msp: Modelspace
        x, y: Position de la fenetre
        width: Largeur (defaut: 1.20m)
        orientation: "horizontal" ou "vertical"
    """
    # TODO: Implementer le dessin de fenetre avec double trait
    # Standard tunisien: hauteur allege 1.00m, hauteur fenetre 1.20m
    if orientation == "horizontal":
        msp.add_line((x, y - 0.05), (x + width, y - 0.05), dxfattribs={"layer": "OUVERTURES"})
        msp.add_line((x, y + 0.05), (x + width, y + 0.05), dxfattribs={"layer": "OUVERTURES"})
    else:
        msp.add_line((x - 0.05, y), (x - 0.05, y + width), dxfattribs={"layer": "OUVERTURES"})
        msp.add_line((x + 0.05, y), (x + 0.05, y + width), dxfattribs={"layer": "OUVERTURES"})


def generate_plan_from_json(plan_data: dict, output_path: str) -> str:
    """
    Genere un fichier DXF a partir d'un JSON de plan.

    Args:
        plan_data: Dictionnaire contenant la definition du plan
        output_path: Chemin du fichier DXF de sortie

    Returns:
        Chemin absolu du fichier genere

    Expected JSON format:
    {
        "terrain": {"width": 15, "height": 25},
        "rooms": [
            {"name": "Salon", "x": 0, "y": 0, "width": 5, "height": 4},
            {"name": "Chambre 1", "x": 5, "y": 0, "width": 4, "height": 3.5}
        ],
        "doors": [...],
        "windows": [...]
    }
    """
    doc = create_document()
    msp = doc.modelspace()

    # Dessiner le terrain
    terrain = plan_data.get("terrain", {})
    if terrain:
        t_width = terrain.get("width", 15)
        t_height = terrain.get("height", 25)
        terrain_pts = create_rectangle_points(-1, -1, t_width + 2, t_height + 2)
        msp.add_lwpolyline(terrain_pts, close=True, dxfattribs={"layer": "TERRAIN"})

    # Dessiner les pieces
    for room in plan_data.get("rooms", []):
        draw_room(
            msp,
            room["x"],
            room["y"],
            room["width"],
            room["height"],
            room["name"],
        )

    # Dessiner les portes
    for door in plan_data.get("doors", []):
        draw_door(msp, door["x"], door["y"], door.get("width", 0.90))

    # Dessiner les fenetres
    for window in plan_data.get("windows", []):
        draw_window(msp, window["x"], window["y"], window.get("width", 1.20))

    # TODO: Ajouter cotations automatiques
    # TODO: Ajouter cartouche (titre, echelle, date, architecte)
    # TODO: Ajouter legende

    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    doc.saveas(str(output))

    return str(output.absolute())


def main():
    """Point d'entree CLI."""
    if len(sys.argv) < 3:
        print("Usage: python -m dxf.generate_dxf --input plan.json --output plan.dxf")
        sys.exit(1)

    input_path = sys.argv[sys.argv.index("--input") + 1]
    output_path = sys.argv[sys.argv.index("--output") + 1]

    with open(input_path, "r") as f:
        plan_data = json.load(f)

    result = generate_plan_from_json(plan_data, output_path)
    print(f"DXF genere: {result}")


if __name__ == "__main__":
    main()
