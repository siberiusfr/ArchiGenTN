package tn.archigen.archigen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.archigen.archigen.dto.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service d'export DXF en Java pur.
 * Genere un fichier DXF R12 (format texte) sans dependance externe.
 * Compatible AutoCAD, LibreCAD, DraftSight, etc.
 */
@Slf4j
@Service
public class DxfExportService {

    // Codes couleur AutoCAD (ACI)
    private static final int COLOR_RED = 1;       // Murs porteurs
    private static final int COLOR_GREEN = 3;     // Cloisons
    private static final int COLOR_BLUE = 5;      // Ouvertures
    private static final int COLOR_WHITE = 7;     // Texte / Cotations
    private static final int COLOR_GREY = 8;      // Mobilier
    private static final int COLOR_ORANGE = 30;   // Terrain

    public byte[] exportToDxf(PlanResponse plan) {
        log.info("Generation DXF: {} pieces, terrain {}x{}m",
                plan.getRooms().size(),
                plan.getTerrain().getWidth(),
                plan.getTerrain().getHeight());

        var sb = new StringBuilder(8192);

        writeHeader(sb);
        writeTables(sb);
        writeEntities(sb, plan);
        writeFooter(sb);

        byte[] dxfBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        log.info("DXF genere: {} octets", dxfBytes.length);
        return dxfBytes;
    }

    // ==================== HEADER ====================

    private void writeHeader(StringBuilder sb) {
        section(sb, "HEADER");
        // Version DXF
        writeGroupCode(sb, 9, "$ACADVER");
        writeGroupCode(sb, 1, "AC1009"); // R12
        // Unites: metres
        writeGroupCode(sb, 9, "$INSUNITS");
        writeGroupCode(sb, 70, "6"); // 6 = metres
        // Limites du dessin
        writeGroupCode(sb, 9, "$LIMMIN");
        writeGroupCode(sb, 10, "0.0");
        writeGroupCode(sb, 20, "0.0");
        writeGroupCode(sb, 9, "$LIMMAX");
        writeGroupCode(sb, 10, "50.0");
        writeGroupCode(sb, 20, "50.0");
        endSection(sb);
    }

    // ==================== TABLES (LAYERS) ====================

    private void writeTables(StringBuilder sb) {
        section(sb, "TABLES");

        // Table des layers
        writeGroupCode(sb, 0, "TABLE");
        writeGroupCode(sb, 2, "LAYER");
        writeGroupCode(sb, 70, "7"); // Nombre de layers

        writeLayer(sb, "TERRAIN", COLOR_ORANGE, "DASHDOT");
        writeLayer(sb, "MURS_PORTEURS", COLOR_RED, "CONTINUOUS");
        writeLayer(sb, "MURS_CLOISONS", COLOR_GREEN, "CONTINUOUS");
        writeLayer(sb, "OUVERTURES", COLOR_BLUE, "CONTINUOUS");
        writeLayer(sb, "COTATIONS", COLOR_WHITE, "CONTINUOUS");
        writeLayer(sb, "TEXTE", COLOR_WHITE, "CONTINUOUS");
        writeLayer(sb, "MOBILIER", COLOR_GREY, "CONTINUOUS");

        writeGroupCode(sb, 0, "ENDTAB");

        endSection(sb);
    }

    private void writeLayer(StringBuilder sb, String name, int color, String linetype) {
        writeGroupCode(sb, 0, "LAYER");
        writeGroupCode(sb, 2, name);
        writeGroupCode(sb, 70, "0");
        writeGroupCode(sb, 62, String.valueOf(color));
        writeGroupCode(sb, 6, linetype);
    }

    // ==================== ENTITIES ====================

    private void writeEntities(StringBuilder sb, PlanResponse plan) {
        section(sb, "ENTITIES");

        // 1. Dessiner le terrain
        drawTerrain(sb, plan.getTerrain());

        // 2. Dessiner les pieces (murs)
        double wt = plan.getWallThickness() != null ? plan.getWallThickness() : 0.20;
        for (var room : plan.getRooms()) {
            drawRoom(sb, room, wt);
        }

        // 3. Dessiner les portes
        if (plan.getDoors() != null) {
            for (var door : plan.getDoors()) {
                drawDoor(sb, door);
            }
        }

        // 4. Dessiner les fenetres
        if (plan.getWindows() != null) {
            for (var window : plan.getWindows()) {
                drawWindow(sb, window);
            }
        }

        // 5. Cotations des pieces
        for (var room : plan.getRooms()) {
            drawRoomDimensions(sb, room);
        }

        // 6. Cartouche (titre)
        drawCartouche(sb, plan);

        endSection(sb);
    }

    private void drawTerrain(StringBuilder sb, TerrainDto terrain) {
        double w = terrain.getWidth();
        double h = terrain.getHeight();
        // Rectangle du terrain avec marge
        drawPolyline(sb, "TERRAIN", new double[][]{
                {-1, -1}, {w + 1, -1}, {w + 1, h + 1}, {-1, h + 1}, {-1, -1}
        });
        // Label terrain
        drawText(sb, "TEXTE", w / 2, -2, 0.3,
                String.format("TERRAIN: %.1fm x %.1fm = %.1fm2", w, h, w * h));
    }

    private void drawRoom(StringBuilder sb, RoomDto room, double wallThickness) {
        double x = room.getX();
        double y = room.getY();
        double w = room.getWidth();
        double h = room.getHeight();

        // Murs exterieurs (porteurs)
        drawPolyline(sb, "MURS_PORTEURS", new double[][]{
                {x, y}, {x + w, y}, {x + w, y + h}, {x, y + h}, {x, y}
        });

        // Murs interieurs (epaisseur)
        double ix = x + wallThickness;
        double iy = y + wallThickness;
        double iw = w - 2 * wallThickness;
        double ih = h - 2 * wallThickness;
        drawPolyline(sb, "MURS_CLOISONS", new double[][]{
                {ix, iy}, {ix + iw, iy}, {ix + iw, iy + ih}, {ix, iy + ih}, {ix, iy}
        });

        // Label piece (nom + surface)
        double innerArea = iw * ih;
        String label = String.format("%s (%.1fm2)", room.getName(), innerArea);
        drawText(sb, "TEXTE", x + w / 2, y + h / 2, 0.15, label);
    }

    private void drawDoor(StringBuilder sb, DoorDto door) {
        double x = door.getX();
        double y = door.getY();
        double w = door.getWidth() != null ? door.getWidth() : 0.90;

        if ("vertical".equals(door.getOrientation())) {
            // Porte verticale: ligne + arc d'ouverture
            drawLine(sb, "OUVERTURES", x, y, x, y + w);
            // Arc d'ouverture (simplifie en quart de cercle via ligne)
            drawArc(sb, "OUVERTURES", x, y, w, 0, 90);
        } else {
            // Porte horizontale
            drawLine(sb, "OUVERTURES", x, y, x + w, y);
            drawArc(sb, "OUVERTURES", x, y, w, 0, 90);
        }
    }

    private void drawWindow(StringBuilder sb, WindowDto window) {
        double x = window.getX();
        double y = window.getY();
        double w = window.getWidth() != null ? window.getWidth() : 1.20;
        double offset = 0.05;

        if ("vertical".equals(window.getOrientation())) {
            // Fenetre verticale: double trait
            drawLine(sb, "OUVERTURES", x - offset, y, x - offset, y + w);
            drawLine(sb, "OUVERTURES", x + offset, y, x + offset, y + w);
        } else {
            // Fenetre horizontale: double trait
            drawLine(sb, "OUVERTURES", x, y - offset, x + w, y - offset);
            drawLine(sb, "OUVERTURES", x, y + offset, x + w, y + offset);
        }
    }

    private void drawRoomDimensions(StringBuilder sb, RoomDto room) {
        double x = room.getX();
        double y = room.getY();
        double w = room.getWidth();
        double h = room.getHeight();

        // Cotation largeur (en bas de la piece)
        drawText(sb, "COTATIONS", x + w / 2, y - 0.3, 0.10,
                String.format("%.2f", w));
        // Cotation hauteur (a gauche de la piece)
        drawText(sb, "COTATIONS", x - 0.5, y + h / 2, 0.10,
                String.format("%.2f", h));
    }

    private void drawCartouche(StringBuilder sb, PlanResponse plan) {
        var terrain = plan.getTerrain();
        double cartX = 0;
        double cartY = -5;

        drawPolyline(sb, "TEXTE", new double[][]{
                {cartX, cartY}, {cartX + 15, cartY}, {cartX + 15, cartY + 3}, {cartX, cartY + 3}, {cartX, cartY}
        });
        drawText(sb, "TEXTE", cartX + 0.5, cartY + 2.2, 0.25, "ArchiGenTN - Plan Architectural");
        drawText(sb, "TEXTE", cartX + 0.5, cartY + 1.4, 0.15,
                String.format("Terrain: %.1fm x %.1fm", terrain.getWidth(), terrain.getHeight()));

        if (plan.getMetrics() != null) {
            drawText(sb, "TEXTE", cartX + 0.5, cartY + 0.7, 0.15,
                    String.format("Surface: %.1fm2 | COS: %.2f | CUF: %.2f",
                            plan.getMetrics().getTotalArea(),
                            plan.getMetrics().getCos(),
                            plan.getMetrics().getCuf()));
        }

        drawText(sb, "TEXTE", cartX + 10, cartY + 0.3, 0.10, "Echelle: 1/100");
    }

    // ==================== DXF PRIMITIVES ====================

    private void drawLine(StringBuilder sb, String layer, double x1, double y1, double x2, double y2) {
        writeGroupCode(sb, 0, "LINE");
        writeGroupCode(sb, 8, layer);
        writeGroupCode(sb, 10, fmt(x1));
        writeGroupCode(sb, 20, fmt(y1));
        writeGroupCode(sb, 30, "0.0");
        writeGroupCode(sb, 11, fmt(x2));
        writeGroupCode(sb, 21, fmt(y2));
        writeGroupCode(sb, 31, "0.0");
    }

    private void drawPolyline(StringBuilder sb, String layer, double[][] points) {
        // Utiliser des LINE individuelles pour compatibilite R12 maximale
        for (int i = 0; i < points.length - 1; i++) {
            drawLine(sb, layer, points[i][0], points[i][1], points[i + 1][0], points[i + 1][1]);
        }
    }

    private void drawText(StringBuilder sb, String layer, double x, double y, double height, String text) {
        writeGroupCode(sb, 0, "TEXT");
        writeGroupCode(sb, 8, layer);
        writeGroupCode(sb, 10, fmt(x));
        writeGroupCode(sb, 20, fmt(y));
        writeGroupCode(sb, 30, "0.0");
        writeGroupCode(sb, 40, fmt(height));
        writeGroupCode(sb, 1, text);
        // Centre horizontal
        writeGroupCode(sb, 72, "1");
        writeGroupCode(sb, 11, fmt(x));
        writeGroupCode(sb, 21, fmt(y));
        writeGroupCode(sb, 31, "0.0");
    }

    private void drawArc(StringBuilder sb, String layer, double cx, double cy, double radius,
                          double startAngle, double endAngle) {
        writeGroupCode(sb, 0, "ARC");
        writeGroupCode(sb, 8, layer);
        writeGroupCode(sb, 10, fmt(cx));
        writeGroupCode(sb, 20, fmt(cy));
        writeGroupCode(sb, 30, "0.0");
        writeGroupCode(sb, 40, fmt(radius));
        writeGroupCode(sb, 50, fmt(startAngle));
        writeGroupCode(sb, 51, fmt(endAngle));
    }

    // ==================== DXF HELPERS ====================

    private void section(StringBuilder sb, String name) {
        writeGroupCode(sb, 0, "SECTION");
        writeGroupCode(sb, 2, name);
    }

    private void endSection(StringBuilder sb) {
        writeGroupCode(sb, 0, "ENDSEC");
    }

    private void writeFooter(StringBuilder sb) {
        writeGroupCode(sb, 0, "EOF");
    }

    private void writeGroupCode(StringBuilder sb, int code, String value) {
        sb.append(String.format("%3d\n%s\n", code, value));
    }

    private String fmt(double value) {
        return String.format("%.4f", value);
    }
}
