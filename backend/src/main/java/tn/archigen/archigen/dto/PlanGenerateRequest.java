package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requete de generation de plan architectural")
public class PlanGenerateRequest {

    @NotNull
    @Valid
    @Schema(description = "Dimensions du terrain")
    private TerrainDto terrain;

    @NotNull
    @Valid
    @Schema(description = "Exigences du plan (pieces, style)")
    private RequirementsDto requirements;

    @Builder.Default
    @Valid
    @Schema(description = "Reglementations urbanistiques (defaut: standard tunisien)")
    private RegulationsDto regulations = new RegulationsDto();
}
