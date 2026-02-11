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
@Schema(description = "Resultat d'analyse structurelle pour un element")
public class StructuralElementResultDto {

    @Schema(description = "Type d'element (poutre, poteau, dalle)", example = "poutre")
    private String elementType;

    @Schema(description = "Identifiant de l'element", example = "Poutre-Salon")
    private String elementId;

    @Schema(description = "Portee en metres", example = "5.5")
    private Double portee;

    @Schema(description = "Moment flechissant max en kN.m", example = "28.36")
    private Double momentMax;

    @Schema(description = "Effort tranchant max en kN", example = "20.63")
    private Double effortTranchant;

    @Schema(description = "Fleche maximale calculee en mm", example = "2.1")
    private Double flecheMax;

    @Schema(description = "Fleche admissible (L/500) en mm", example = "11.0")
    private Double flecheAdmissible;

    @Schema(description = "Hauteur de poutre recommandee en metres", example = "0.35")
    private Double hauteurRecommandee;

    @Schema(description = "Element conforme", example = "true")
    private Boolean valid;

    @Schema(description = "Message de diagnostic")
    private String message;
}
