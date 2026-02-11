package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reponse combinee: plan genere + analyse structurelle")
public record GenerateAndAnalyzeResponse(
        @Schema(description = "Plan architectural genere") PlanResponse plan,
        @Schema(description = "Rapport d'analyse structurelle") StructuralAnalysisResponse analysis
) {}
