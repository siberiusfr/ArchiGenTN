package tn.archigen.archigen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.archigen.archigen.dto.*;
import tn.archigen.archigen.service.ClaudeAiPlanService;
import tn.archigen.archigen.service.DxfExportService;
import tn.archigen.archigen.service.StructuralAnalysisService;

@Slf4j
@RestController
@RequestMapping("/api/plans/ai")
@RequiredArgsConstructor
@Tag(name = "Plans IA", description = "Generation de plans architecturaux via Claude AI (avec fallback algorithmique)")
public class AiPlanController {

    private final ClaudeAiPlanService claudeAiPlanService;
    private final DxfExportService dxfExportService;
    private final StructuralAnalysisService structuralAnalysisService;

    // ==================== GENERATION IA ====================

    @PostMapping("/generate")
    @Operation(
            summary = "Generer un plan via Claude AI",
            description = "Utilise Claude AI pour generer un plan architectural intelligent. "
                    + "Si l'API key est absente ou en cas d'erreur, retombe sur l'algorithme classique. "
                    + "Retourne le meme format PlanResponse, compatible avec /export/dxf et /analyze."
    )
    @ApiResponse(responseCode = "200", description = "Plan genere (IA ou fallback)")
    @ApiResponse(responseCode = "400", description = "Parametres invalides")
    public ResponseEntity<PlanResponse> generatePlan(@Valid @RequestBody PlanGenerateRequest request) {
        log.info("POST /api/plans/ai/generate - terrain: {}x{}, pieces: {}",
                request.getTerrain().getWidth(),
                request.getTerrain().getHeight(),
                request.getRequirements().getRooms().size());

        PlanResponse plan = claudeAiPlanService.generatePlan(request);
        return ResponseEntity.ok(plan);
    }

    // ==================== GENERATION IA + EXPORT DXF ====================

    @PostMapping("/generate-dxf")
    @Operation(
            summary = "Generer un plan IA ET exporter en DXF",
            description = "Combine generation AI et export DXF en un seul appel. "
                    + "Retourne directement le fichier DXF."
    )
    @ApiResponse(responseCode = "200", description = "Fichier DXF genere",
            content = @Content(mediaType = "application/dxf", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> generateAndExportDxf(@Valid @RequestBody PlanGenerateRequest request) {
        log.info("POST /api/plans/ai/generate-dxf - terrain: {}x{}", request.getTerrain().getWidth(), request.getTerrain().getHeight());

        PlanResponse plan = claudeAiPlanService.generatePlan(request);
        byte[] dxfContent = dxfExportService.exportToDxf(plan);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archigentn-ai-plan.dxf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/dxf")
                .contentLength(dxfContent.length)
                .body(dxfContent);
    }

    // ==================== GENERATION IA + ANALYSE ====================

    @PostMapping("/generate-and-analyze")
    @Operation(
            summary = "Generer un plan IA ET analyser sa structure",
            description = "Combine generation AI et analyse structurelle. "
                    + "Retourne le plan genere + le rapport structurel."
    )
    @ApiResponse(responseCode = "200", description = "Plan et analyse structurelle")
    public ResponseEntity<GenerateAndAnalyzeResponse> generateAndAnalyze(
            @Valid @RequestBody PlanGenerateRequest request,
            @RequestParam(defaultValue = "habitation")
            @Schema(description = "Type de batiment", allowableValues = {"habitation", "bureaux", "commerce"})
            String buildingType
    ) {
        log.info("POST /api/plans/ai/generate-and-analyze");

        PlanResponse plan = claudeAiPlanService.generatePlan(request);
        StructuralAnalysisResponse analysis = structuralAnalysisService.analyze(plan, buildingType);

        return ResponseEntity.ok(new GenerateAndAnalyzeResponse(plan, analysis));
    }
}
