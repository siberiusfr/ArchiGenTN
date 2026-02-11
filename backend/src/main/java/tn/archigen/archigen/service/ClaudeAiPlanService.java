package tn.archigen.archigen.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tn.archigen.archigen.config.AiProperties;
import tn.archigen.archigen.dto.*;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ClaudeAiPlanService {

    private final AiProperties aiProperties;
    private final PlanGenerationService fallbackService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(\\{.*?})\\s*```", Pattern.DOTALL);

    public ClaudeAiPlanService(AiProperties aiProperties,
                               PlanGenerationService fallbackService,
                               ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.fallbackService = fallbackService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    public PlanResponse generatePlan(PlanGenerateRequest request) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            log.info("Pas de cle API AI configuree, utilisation de l'algorithme classique");
            return fallbackService.generatePlan(request);
        }

        try {
            return callClaudeApi(request);
        } catch (Exception e) {
            log.warn("Erreur appel Claude AI, fallback sur algorithme classique: {}", e.getMessage());
            return fallbackService.generatePlan(request);
        }
    }

    private PlanResponse callClaudeApi(PlanGenerateRequest request) {
        var terrain = request.getTerrain();
        var requirements = request.getRequirements();
        var regulations = request.getRegulations() != null ? request.getRegulations() : new RegulationsDto();

        double buildableWidth = terrain.getWidth() - 2 * regulations.getRetraitLateral();
        double buildableHeight = terrain.getHeight() - regulations.getRetraitFrontal() - regulations.getRetraitArriere();
        double buildableX = regulations.getRetraitLateral();
        double buildableY = regulations.getRetraitFrontal();

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request, buildableX, buildableY, buildableWidth, buildableHeight);

        log.debug("Appel Claude API - model: {}, terrain: {}x{}", aiProperties.getModel(), terrain.getWidth(), terrain.getHeight());

        var messages = List.of(new ClaudeMessage("user", userPrompt));
        var apiRequest = new ClaudeApiRequest(
                aiProperties.getModel(),
                aiProperties.getMaxTokens(),
                aiProperties.getTemperature(),
                systemPrompt,
                messages
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(apiRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur serialisation requete Claude", e);
        }

        String responseBody = restClient.post()
                .uri(aiProperties.getApiUrl())
                .header("x-api-key", aiProperties.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        log.debug("Reponse Claude recue ({} chars)", responseBody != null ? responseBody.length() : 0);

        return parseClaudeResponse(responseBody, request);
    }

    private String buildSystemPrompt() {
        return """
                Tu es un architecte tunisien expert en conception de plans de maisons.
                Tu generes des plans architecturaux au format JSON strict.

                Regles de conception:
                - Murs porteurs: 20cm (0.20m) d'epaisseur (standard tunisien)
                - Toutes les coordonnees sont en metres
                - Les pieces doivent etre des rectangles positionnes sans chevauchement
                - L'entree principale doit etre accessible depuis le retrait frontal
                - Les pieces humides (cuisine, sdb, wc) doivent etre regroupees pour optimiser la plomberie
                - Le salon est la piece principale, bien eclaire avec fenetres sur facade
                - Les chambres doivent avoir des fenetres sur murs exterieurs
                - Prevoir des portes entre pieces adjacentes
                - Prevoir des fenetres sur les murs exterieurs (pas pour sdb, wc, couloir)

                Styles architecturaux:
                - moderne: formes epurees, grands espaces ouverts, baies vitrees
                - traditionnel: patio central (west ed-dar), pieces autour du patio
                - neo_mauresque: arcs, proportions harmonieuses, symetrie
                - colonial: plan lineaire, hauts plafonds, veranda
                - mediterraneen: volumes compacts, terrasse, loggia

                Normes tunisiennes (PAU):
                - COS (Coefficient d'Occupation du Sol): surface batie / surface terrain
                - CUF (Coefficient d'Utilisation Fonciere): surface plancher totale / surface terrain
                - Retraits obligatoires: frontal (5m), lateral (3m), arriere (3m)

                Tu dois repondre UNIQUEMENT avec un bloc JSON valide, sans texte avant ou apres.
                """;
    }

    private String buildUserPrompt(PlanGenerateRequest request,
                                   double buildableX, double buildableY,
                                   double buildableWidth, double buildableHeight) {
        var terrain = request.getTerrain();
        var requirements = request.getRequirements();
        var regulations = request.getRegulations() != null ? request.getRegulations() : new RegulationsDto();

        StringBuilder rooms = new StringBuilder();
        for (var room : requirements.getRooms()) {
            int count = room.getCount() != null ? room.getCount() : 1;
            String name = room.getName() != null ? room.getName() : room.getType();
            double minArea = room.getMinArea() != null ? room.getMinArea() : 12.0;
            rooms.append(String.format("  - %s (type: %s, surface min: %.1fm2, quantite: %d)%n", name, room.getType(), minArea, count));
        }

        return String.format("""
                Genere un plan architectural pour une maison tunisienne.

                TERRAIN:
                - Dimensions: %.1fm x %.1fm (largeur x profondeur)
                - Surface totale: %.1fm2

                ZONE CONSTRUCTIBLE (apres retraits):
                - Origine: x=%.2f, y=%.2f
                - Dimensions: %.2fm x %.2fm
                - COS max: %.2f, CUF max: %.2f

                PIECES DEMANDEES:
                %s
                STYLE: %s
                ETAGES: %d

                IMPORTANT - Format de reponse JSON strict:
                {
                  "terrain": {"width": %.1f, "height": %.1f},
                  "rooms": [
                    {"name": "Salon", "type": "salon", "x": 3.0, "y": 5.0, "width": 5.0, "height": 4.0}
                  ],
                  "doors": [
                    {"x": 5.0, "y": 5.0, "width": 1.0, "orientation": "horizontal"}
                  ],
                  "windows": [
                    {"x": 4.5, "y": 9.0, "width": 1.2, "orientation": "horizontal"}
                  ],
                  "wallThickness": 0.20,
                  "metrics": {
                    "totalArea": 85.5,
                    "cos": 0.30,
                    "cuf": 0.30,
                    "regulationsCompliant": true,
                    "complianceMessage": null
                  }
                }

                Contraintes:
                - Toutes les pieces dans la zone constructible [x: %.2f-%.2f, y: %.2f-%.2f]
                - Pas de chevauchement entre pieces
                - Porte principale (width: 1.0) sur le mur frontal (y min) de l'entree ou du salon
                - Portes interieures (width: 0.90) entre pieces adjacentes
                - Fenetres (width: 1.20) sur murs exterieurs (sauf sdb, wc, couloir)
                - Coordonnees arrondies a 10cm (0.1m)
                """,
                terrain.getWidth(), terrain.getHeight(),
                terrain.getWidth() * terrain.getHeight(),
                buildableX, buildableY,
                buildableWidth, buildableHeight,
                regulations.getCos(), regulations.getCuf(),
                rooms,
                requirements.getStyle() != null ? requirements.getStyle() : "moderne",
                requirements.getFloors() != null ? requirements.getFloors() : 1,
                terrain.getWidth(), terrain.getHeight(),
                buildableX, buildableX + buildableWidth,
                buildableY, buildableY + buildableHeight
        );
    }

    private PlanResponse parseClaudeResponse(String responseBody, PlanGenerateRequest request) {
        try {
            ClaudeApiResponse apiResponse = objectMapper.readValue(responseBody, ClaudeApiResponse.class);

            if (apiResponse.content() == null || apiResponse.content().isEmpty()) {
                throw new RuntimeException("Reponse Claude vide");
            }

            String textContent = apiResponse.content().stream()
                    .filter(c -> "text".equals(c.type()))
                    .map(ClaudeContentBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Pas de contenu texte dans la reponse Claude"));

            String json = extractJson(textContent);
            PlanResponse plan = objectMapper.readValue(json, PlanResponse.class);

            validatePlan(plan, request);

            log.info("Plan AI genere: {} pieces, surface = {}m2",
                    plan.getRooms().size(),
                    plan.getMetrics() != null ? plan.getMetrics().getTotalArea() : "N/A");

            return plan;

        } catch (Exception e) {
            throw new RuntimeException("Erreur parsing reponse Claude: " + e.getMessage(), e);
        }
    }

    private String extractJson(String text) {
        // Essayer d'extraire d'un bloc markdown ```json ... ```
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Sinon, chercher le premier { et le dernier }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        throw new RuntimeException("Pas de JSON trouve dans la reponse Claude");
    }

    private void validatePlan(PlanResponse plan, PlanGenerateRequest request) {
        if (plan.getRooms() == null || plan.getRooms().isEmpty()) {
            throw new RuntimeException("Le plan AI ne contient aucune piece");
        }

        var terrain = request.getTerrain();
        for (var room : plan.getRooms()) {
            if (room.getX() + room.getWidth() > terrain.getWidth() ||
                    room.getY() + room.getHeight() > terrain.getHeight() ||
                    room.getX() < 0 || room.getY() < 0) {
                log.warn("Piece '{}' hors terrain, le plan sera quand meme retourne", room.getName());
            }
        }

        // Remplir le terrain si absent
        if (plan.getTerrain() == null) {
            plan.setTerrain(request.getTerrain());
        }
    }

    // ==================== Inner records pour API Claude ====================

    record ClaudeApiRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature,
            String system,
            List<ClaudeMessage> messages
    ) {}

    record ClaudeMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeApiResponse(
            String id,
            String type,
            String model,
            List<ClaudeContentBlock> content,
            @JsonProperty("stop_reason") String stopReason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClaudeContentBlock(String type, String text) {}
}
