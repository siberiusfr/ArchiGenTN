package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plan architectural genere (sert aussi d'input pour export DXF et analyse structurelle)")
public class PlanResponse {

    @Schema(description = "Dimensions du terrain")
    private TerrainDto terrain;

    @Schema(description = "Pieces positionnees dans le plan")
    private List<RoomDto> rooms;

    @Schema(description = "Portes positionnees")
    private List<DoorDto> doors;

    @Schema(description = "Fenetres positionnees")
    private List<WindowDto> windows;

    @Builder.Default
    @Schema(description = "Epaisseur des murs en metres (standard TN: 0.20m)", example = "0.20")
    private Double wallThickness = 0.20;

    @Schema(description = "Metriques calculees")
    private PlanMetricsDto metrics;
}
