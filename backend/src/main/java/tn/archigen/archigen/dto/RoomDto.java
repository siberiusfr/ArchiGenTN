package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Piece positionnee dans le plan")
public class RoomDto {

    @NotBlank
    @Schema(description = "Nom de la piece", example = "Salon")
    private String name;

    @NotBlank
    @Schema(description = "Type de piece", example = "salon")
    private String type;

    @NotNull
    @Schema(description = "Position X (coin inferieur gauche) en metres", example = "5.0")
    private Double x;

    @NotNull
    @Schema(description = "Position Y (coin inferieur gauche) en metres", example = "5.0")
    private Double y;

    @NotNull
    @Schema(description = "Largeur de la piece en metres", example = "5.5")
    private Double width;

    @NotNull
    @Schema(description = "Hauteur (profondeur) de la piece en metres", example = "4.8")
    private Double height;
}
