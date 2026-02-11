package tn.archigen.archigen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.archigen.archigen.dto.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service de generation de plans architecturaux.
 * Algorithme de placement de pieces en rangees (row-packing).
 * TODO: Remplacer par un appel IA (Claude/OpenAI) pour un placement intelligent.
 */
@Slf4j
@Service
public class PlanGenerationService {

    private static final double WALL_THICKNESS = 0.20; // 20cm standard tunisien
    private static final double DOOR_WIDTH_INTERIOR = 0.90;
    private static final double DOOR_WIDTH_MAIN = 1.00;
    private static final double WINDOW_WIDTH = 1.20;

    // Surfaces minimales par type de piece (normes tunisiennes habitation)
    private static final java.util.Map<String, Double> DEFAULT_MIN_AREAS = java.util.Map.of(
            "salon", 20.0,
            "chambre", 12.0,
            "cuisine", 9.0,
            "sdb", 5.0,
            "wc", 2.5,
            "entree", 4.0,
            "couloir", 3.0,
            "bureau", 10.0,
            "garage", 15.0
    );

    public PlanResponse generatePlan(PlanGenerateRequest request) {
        var terrain = request.getTerrain();
        var requirements = request.getRequirements();
        var regulations = request.getRegulations() != null ? request.getRegulations() : new RegulationsDto();

        // Calculer la zone constructible (terrain - retraits)
        double buildableX = regulations.getRetraitLateral();
        double buildableY = regulations.getRetraitFrontal();
        double buildableWidth = terrain.getWidth() - 2 * regulations.getRetraitLateral();
        double buildableHeight = terrain.getHeight() - regulations.getRetraitFrontal() - regulations.getRetraitArriere();

        // Verifier COS: surface batie max
        double maxBuildArea = terrain.getWidth() * terrain.getHeight() * regulations.getCos();
        double targetArea = requirements.getTotalArea() != null ? requirements.getTotalArea() : maxBuildArea * 0.8;
        double actualBuildArea = Math.min(targetArea, maxBuildArea);

        // Expander les pieces (gerer count > 1)
        List<RoomRequirementDto> expandedRooms = expandRoomRequirements(requirements.getRooms());

        // Placer les pieces avec algorithme row-packing
        List<RoomDto> placedRooms = placeRooms(expandedRooms, buildableX, buildableY, buildableWidth, buildableHeight, actualBuildArea);

        // Generer les portes et fenetres
        List<DoorDto> doors = generateDoors(placedRooms, buildableX, buildableY);
        List<WindowDto> windows = generateWindows(placedRooms, buildableX, buildableY, buildableWidth, buildableHeight);

        // Calculer les metriques
        PlanMetricsDto metrics = calculateMetrics(placedRooms, terrain, regulations);

        return PlanResponse.builder()
                .terrain(terrain)
                .rooms(placedRooms)
                .doors(doors)
                .windows(windows)
                .wallThickness(WALL_THICKNESS)
                .metrics(metrics)
                .build();
    }

    private List<RoomRequirementDto> expandRoomRequirements(List<RoomRequirementDto> rooms) {
        List<RoomRequirementDto> expanded = new ArrayList<>();
        for (var room : rooms) {
            int count = room.getCount() != null ? room.getCount() : 1;
            for (int i = 0; i < count; i++) {
                String name = room.getName() != null ? room.getName() : capitalize(room.getType());
                if (count > 1) {
                    name = name + " " + (i + 1);
                }
                expanded.add(RoomRequirementDto.builder()
                        .type(room.getType())
                        .name(name)
                        .minArea(room.getMinArea() != null ? room.getMinArea() : DEFAULT_MIN_AREAS.getOrDefault(room.getType(), 12.0))
                        .count(1)
                        .build());
            }
        }
        return expanded;
    }

    /**
     * Algorithme de placement row-packing.
     * Place les pieces de gauche a droite, puis passe a la rangee suivante.
     */
    private List<RoomDto> placeRooms(
            List<RoomRequirementDto> rooms,
            double startX, double startY,
            double maxWidth, double maxHeight,
            double targetArea
    ) {
        List<RoomDto> placed = new ArrayList<>();

        // Trier par surface decroissante (les grandes pieces d'abord)
        List<RoomRequirementDto> sorted = new ArrayList<>(rooms);
        sorted.sort((a, b) -> Double.compare(
                b.getMinArea() != null ? b.getMinArea() : 12.0,
                a.getMinArea() != null ? a.getMinArea() : 12.0
        ));

        double currentX = startX;
        double currentY = startY;
        double rowHeight = 0;
        double totalPlacedArea = 0;

        for (var room : sorted) {
            double area = room.getMinArea() != null ? room.getMinArea() : DEFAULT_MIN_AREAS.getOrDefault(room.getType(), 12.0);

            // Calculer dimensions rectangulaires (ratio ~1.3 pour les grandes pieces, ~1.5 pour les petites)
            double ratio = area > 15 ? 1.3 : 1.5;
            double roomHeight = Math.sqrt(area / ratio);
            double roomWidth = area / roomHeight;

            // Arrondir a 10cm pres
            roomWidth = Math.round(roomWidth * 10) / 10.0;
            roomHeight = Math.round(roomHeight * 10) / 10.0;

            // Verifier si la piece tient dans la rangee courante
            if (currentX + roomWidth > startX + maxWidth) {
                // Passer a la rangee suivante
                currentX = startX;
                currentY += rowHeight + WALL_THICKNESS;
                rowHeight = 0;
            }

            // Verifier si on depasse la hauteur max
            if (currentY + roomHeight > startY + maxHeight) {
                log.warn("Plus de place pour la piece: {} ({}m2)", room.getName(), area);
                break;
            }

            placed.add(RoomDto.builder()
                    .name(room.getName())
                    .type(room.getType())
                    .x(currentX)
                    .y(currentY)
                    .width(roomWidth)
                    .height(roomHeight)
                    .build());

            totalPlacedArea += roomWidth * roomHeight;
            rowHeight = Math.max(rowHeight, roomHeight);
            currentX += roomWidth + WALL_THICKNESS;
        }

        log.info("Placement termine: {} pieces, surface totale = {}m2", placed.size(), Math.round(totalPlacedArea * 10) / 10.0);
        return placed;
    }

    private List<DoorDto> generateDoors(List<RoomDto> rooms, double buildableX, double buildableY) {
        List<DoorDto> doors = new ArrayList<>();

        for (int i = 0; i < rooms.size(); i++) {
            var room = rooms.get(i);

            // Porte d'entree pour la premiere piece (porte principale)
            if (i == 0) {
                doors.add(DoorDto.builder()
                        .x(room.getX() + room.getWidth() / 2 - DOOR_WIDTH_MAIN / 2)
                        .y(room.getY())
                        .width(DOOR_WIDTH_MAIN)
                        .orientation("horizontal")
                        .build());
            }

            // Porte interieure sur le mur droit (vers piece suivante si adjacente)
            if (i < rooms.size() - 1) {
                var nextRoom = rooms.get(i + 1);
                // Si les pieces sont adjacentes horizontalement
                if (Math.abs(room.getY() - nextRoom.getY()) < 0.5) {
                    double doorY = Math.max(room.getY(), nextRoom.getY()) + 0.5;
                    doors.add(DoorDto.builder()
                            .x(room.getX() + room.getWidth())
                            .y(doorY)
                            .width(DOOR_WIDTH_INTERIOR)
                            .orientation("vertical")
                            .build());
                }
            }
        }

        return doors;
    }

    private List<WindowDto> generateWindows(
            List<RoomDto> rooms,
            double buildableX, double buildableY,
            double buildableWidth, double buildableHeight
    ) {
        List<WindowDto> windows = new ArrayList<>();

        for (var room : rooms) {
            // Pas de fenetres pour SDB, WC, couloir
            if ("sdb".equals(room.getType()) || "wc".equals(room.getType()) || "couloir".equals(room.getType())) {
                continue;
            }

            // Fenetre sur le mur du haut (si c'est un mur exterieur)
            if (room.getY() + room.getHeight() >= buildableY + buildableHeight - 0.5) {
                windows.add(WindowDto.builder()
                        .x(room.getX() + room.getWidth() / 2 - WINDOW_WIDTH / 2)
                        .y(room.getY() + room.getHeight())
                        .width(WINDOW_WIDTH)
                        .orientation("horizontal")
                        .build());
            }

            // Fenetre sur le mur gauche (si mur exterieur)
            if (Math.abs(room.getX() - buildableX) < 0.5) {
                windows.add(WindowDto.builder()
                        .x(room.getX())
                        .y(room.getY() + room.getHeight() / 2 - WINDOW_WIDTH / 2)
                        .width(WINDOW_WIDTH)
                        .orientation("vertical")
                        .build());
            }

            // Fenetre sur le mur droit (si mur exterieur)
            if (Math.abs(room.getX() + room.getWidth() - (buildableX + buildableWidth)) < 0.5) {
                windows.add(WindowDto.builder()
                        .x(room.getX() + room.getWidth())
                        .y(room.getY() + room.getHeight() / 2 - WINDOW_WIDTH / 2)
                        .width(WINDOW_WIDTH)
                        .orientation("vertical")
                        .build());
            }
        }

        return windows;
    }

    private PlanMetricsDto calculateMetrics(List<RoomDto> rooms, TerrainDto terrain, RegulationsDto regulations) {
        double totalArea = rooms.stream()
                .mapToDouble(r -> r.getWidth() * r.getHeight())
                .sum();

        double terrainArea = terrain.getWidth() * terrain.getHeight();
        double cos = totalArea / terrainArea;
        double cuf = totalArea / terrainArea; // Pour RDC seul, CUF = COS

        boolean compliant = cos <= regulations.getCos() && cuf <= regulations.getCuf();
        String message = null;
        if (!compliant) {
            var messages = new ArrayList<String>();
            if (cos > regulations.getCos()) {
                messages.add(String.format("COS %.2f depasse le max autorise %.2f", cos, regulations.getCos()));
            }
            if (cuf > regulations.getCuf()) {
                messages.add(String.format("CUF %.2f depasse le max autorise %.2f", cuf, regulations.getCuf()));
            }
            message = String.join(". ", messages);
        }

        return PlanMetricsDto.builder()
                .totalArea(Math.round(totalArea * 100) / 100.0)
                .cos(Math.round(cos * 100) / 100.0)
                .cuf(Math.round(cuf * 100) / 100.0)
                .regulationsCompliant(compliant)
                .complianceMessage(message)
                .build();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
