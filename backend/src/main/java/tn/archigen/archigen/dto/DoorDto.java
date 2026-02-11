package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Porte positionnee dans le plan")
public class DoorDto {

    @Schema(description = "Position X en metres", example = "7.0")
    private Double x;

    @Schema(description = "Position Y en metres", example = "5.0")
    private Double y;

    @Builder.Default
    @Schema(description = "Largeur de la porte en metres (standard TN: 0.90m interieur, 1.00m principale)", example = "0.90")
    private Double width = 0.90;

    @Builder.Default
    @Schema(description = "Orientation", example = "horizontal", allowableValues = {"horizontal", "vertical"})
    private String orientation = "horizontal";
}
