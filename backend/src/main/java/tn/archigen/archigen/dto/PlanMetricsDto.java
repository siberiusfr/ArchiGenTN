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
@Schema(description = "Metriques calculees du plan")
public class PlanMetricsDto {

    @Schema(description = "Surface totale construite en m2", example = "122.5")
    private Double totalArea;

    @Schema(description = "COS calcule (surface batie / surface terrain)", example = "0.33")
    private Double cos;

    @Schema(description = "CUF calcule (surface plancher / surface terrain)", example = "0.33")
    private Double cuf;

    @Schema(description = "Conformite aux reglementations", example = "true")
    private Boolean regulationsCompliant;

    @Schema(description = "Message si non conforme")
    private String complianceMessage;
}
