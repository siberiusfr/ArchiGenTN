package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exigences du plan architectural")
public class RequirementsDto {

    @Schema(description = "Surface totale souhaitee en m2", example = "120.0")
    private Double totalArea;

    @Builder.Default
    @Schema(description = "Nombre d'etages", example = "1")
    private Integer floors = 1;

    @NotEmpty
    @Valid
    @Schema(description = "Liste des pieces souhaitees")
    private List<RoomRequirementDto> rooms;

    @Builder.Default
    @Schema(description = "Style architectural", example = "moderne",
            allowableValues = {"moderne", "traditionnel", "neo_mauresque", "colonial", "mediterraneen"})
    private String style = "moderne";
}
