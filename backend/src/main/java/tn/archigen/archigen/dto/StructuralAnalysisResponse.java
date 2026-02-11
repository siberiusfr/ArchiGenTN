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
@Schema(description = "Rapport d'analyse structurelle")
public class StructuralAnalysisResponse {

    @Builder.Default
    @Schema(description = "Type de batiment", example = "habitation")
    private String buildingType = "habitation";

    @Schema(description = "Charge totale appliquee en kN/m2", example = "7.5")
    private Double chargeTotale;

    @Schema(description = "Resultats par element structurel")
    private List<StructuralElementResultDto> elements;

    @Schema(description = "Structure globalement conforme", example = "true")
    private Boolean globalValid;

    @Schema(description = "Resume de l'analyse")
    private String summary;
}
