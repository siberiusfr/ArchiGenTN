package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exigence pour une piece")
public class RoomRequirementDto {

    @NotBlank
    @Schema(description = "Type de piece", example = "salon",
            allowableValues = {"salon", "chambre", "cuisine", "sdb", "wc", "entree", "couloir", "bureau", "garage"})
    private String type;

    @Schema(description = "Nom personnalise de la piece", example = "Salon")
    private String name;

    @DecimalMin("4.0")
    @Schema(description = "Surface minimale souhaitee en m2", example = "25.0")
    private Double minArea;

    @Builder.Default
    @Schema(description = "Nombre de pieces de ce type", example = "1")
    private Integer count = 1;
}
