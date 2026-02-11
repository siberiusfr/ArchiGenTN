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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.archigen.archigen.dto.*;
import tn.archigen.archigen.service.DxfExportService;
import tn.archigen.archigen.service.PlanGenerationService;
import tn.archigen.archigen.service.StructuralAnalysisService;

@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Generation de plans architecturaux, export DXF et analyse structurelle")
public class PlanController {

    private final PlanGenerationService planGenerationService;
    private final DxfExportService dxfExportService;
    private final StructuralAnalysisService structuralAnalysisService;

    // ==================== GENERATION ====================

    @PostMapping("/generate")
    @Operation(
            summary = "Generer un plan architectural",
            description = "Genere un plan a partir des exigences (terrain, pieces, reglementations). "
                    + "Retourne les pieces positionnees avec portes, fenetres et metriques. "
                    + "Le JSON de reponse peut etre reutilise directement dans /export/dxf et /analyze."
    )
    @ApiResponse(responseCode = "200", description = "Plan genere avec succes")
    @ApiResponse(responseCode = "400", description = "Parametres invalides")
    public ResponseEntity<PlanResponse> generatePlan(@Valid @RequestBody PlanGenerateRequest request) {
        log.info("POST /api/plans/generate - terrain: {}x{}, pieces: {}",
                request.getTerrain().getWidth(),
                request.getTerrain().getHeight(),
                request.getRequirements().getRooms().size());

        PlanResponse plan = planGenerationService.generatePlan(request);
        return ResponseEntity.ok(plan);
    }

    // ==================== EXPORT DXF ====================

    @PostMapping("/export/dxf")
    @Operation(
            summary = "Exporter un plan en DXF (AutoCAD)",
            description = "Prend un plan (JSON de PlanResponse) et genere un fichier DXF telechareable. "
                    + "Le DXF est compatible AutoCAD, LibreCAD, DraftSight. "
                    + "Utilisez la reponse de /generate comme input."
    )
    @ApiResponse(responseCode = "200", description = "Fichier DXF genere",
            content = @Content(mediaType = "application/dxf", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "Plan invalide")
    public ResponseEntity<byte[]> exportDxf(@Valid @RequestBody PlanResponse plan) {
        log.info("POST /api/plans/export/dxf - {} pieces", plan.getRooms().size());

        byte[] dxfContent = dxfExportService.exportToDxf(plan);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archigentn-plan.dxf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/dxf")
                .contentLength(dxfContent.length)
                .body(dxfContent);
    }

    // ==================== GENERATION + EXPORT COMBINE ====================

    @PostMapping("/generate-dxf")
    @Operation(
            summary = "Generer un plan ET exporter directement en DXF",
            description = "Combine /generate et /export/dxf en un seul appel. "
                    + "Prend les exigences et retourne directement le fichier DXF."
    )
    @ApiResponse(responseCode = "200", description = "Fichier DXF genere",
            content = @Content(mediaType = "application/dxf", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> generateAndExportDxf(@Valid @RequestBody PlanGenerateRequest request) {
        log.info("POST /api/plans/generate-dxf - terrain: {}x{}", request.getTerrain().getWidth(), request.getTerrain().getHeight());

        PlanResponse plan = planGenerationService.generatePlan(request);
        byte[] dxfContent = dxfExportService.exportToDxf(plan);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archigentn-plan.dxf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/dxf")
                .contentLength(dxfContent.length)
                .body(dxfContent);
    }

    // ==================== ANALYSE STRUCTURELLE ====================

    @PostMapping("/analyze")
    @Operation(
            summary = "Analyse structurelle d'un plan",
            description = "Effectue un pre-dimensionnement structurel (poutres BA) selon les normes tunisiennes. "
                    + "Verifie les portees, fleches et efforts pour chaque piece. "
                    + "Utilisez la reponse de /generate comme input."
    )
    @ApiResponse(responseCode = "200", description = "Rapport d'analyse structurelle")
    public ResponseEntity<StructuralAnalysisResponse> analyzePlan(
            @Valid @RequestBody PlanResponse plan,
            @RequestParam(defaultValue = "habitation")
            @Schema(description = "Type de batiment", allowableValues = {"habitation", "bureaux", "commerce"})
            String buildingType
    ) {
        log.info("POST /api/plans/analyze - {} pieces, type: {}", plan.getRooms().size(), buildingType);

        StructuralAnalysisResponse report = structuralAnalysisService.analyze(plan, buildingType);
        return ResponseEntity.ok(report);
    }

    // ==================== GENERATION + ANALYSE COMBINE ====================

    @PostMapping("/generate-and-analyze")
    @Operation(
            summary = "Generer un plan ET analyser sa structure",
            description = "Combine /generate et /analyze. Retourne le plan genere + le rapport structurel."
    )
    @ApiResponse(responseCode = "200", description = "Plan et analyse structurelle")
    public ResponseEntity<GenerateAndAnalyzeResponse> generateAndAnalyze(
            @Valid @RequestBody PlanGenerateRequest request,
            @RequestParam(defaultValue = "habitation") String buildingType
    ) {
        log.info("POST /api/plans/generate-and-analyze");

        PlanResponse plan = planGenerationService.generatePlan(request);
        StructuralAnalysisResponse analysis = structuralAnalysisService.analyze(plan, buildingType);

        return ResponseEntity.ok(new GenerateAndAnalyzeResponse(plan, analysis));
    }

    /**
     * Reponse combinee plan + analyse (inner record).
     */
    @Schema(description = "Reponse combinee: plan genere + analyse structurelle")
    public record GenerateAndAnalyzeResponse(
            @Schema(description = "Plan architectural genere") PlanResponse plan,
            @Schema(description = "Rapport d'analyse structurelle") StructuralAnalysisResponse analysis
    ) {}
}
