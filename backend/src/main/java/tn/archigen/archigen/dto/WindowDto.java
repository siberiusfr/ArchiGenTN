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
@Schema(description = "Fenetre positionnee dans le plan")
public class WindowDto {

    @Schema(description = "Position X en metres", example = "6.0")
    private Double x;

    @Schema(description = "Position Y en metres", example = "9.8")
    private Double y;

    @Builder.Default
    @Schema(description = "Largeur de la fenetre en metres", example = "1.20")
    private Double width = 1.20;

    @Builder.Default
    @Schema(description = "Orientation", example = "horizontal", allowableValues = {"horizontal", "vertical"})
    private String orientation = "horizontal";
}
