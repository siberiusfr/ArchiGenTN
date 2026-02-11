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
@Schema(description = "Reglementations urbanistiques tunisiennes (PAU)")
public class RegulationsDto {

    @Builder.Default
    @Schema(description = "Coefficient d'Occupation du Sol max", example = "0.40")
    private Double cos = 0.40;

    @Builder.Default
    @Schema(description = "Coefficient d'Utilisation Fonciere max", example = "1.20")
    private Double cuf = 1.20;

    @Builder.Default
    @Schema(description = "Retrait frontal minimum en metres", example = "5.0")
    private Double retraitFrontal = 5.0;

    @Builder.Default
    @Schema(description = "Retrait lateral minimum en metres", example = "3.0")
    private Double retraitLateral = 3.0;

    @Builder.Default
    @Schema(description = "Retrait arriere minimum en metres", example = "3.0")
    private Double retraitArriere = 3.0;

    @Builder.Default
    @Schema(description = "Hauteur maximale du batiment en metres", example = "9.0")
    private Double hauteurMax = 9.0;
}
