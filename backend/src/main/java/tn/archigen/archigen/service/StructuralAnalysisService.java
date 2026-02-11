package tn.archigen.archigen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.archigen.archigen.dto.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service d'analyse structurelle simplifiee.
 * Pre-dimensionnement des poutres BA selon les normes tunisiennes.
 *
 * Normes de reference:
 * - Charges permanentes: DTU P06-001 (adapte Tunisie)
 * - Beton: C25/30 (fc28 = 25 MPa) - standard Tunisie
 * - Acier: HA400 (fe = 400 MPa)
 */
@Slf4j
@Service
public class StructuralAnalysisService {

    // Proprietes materiaux
    private static final double E_BETON = 31_000.0;  // Module elasticite beton C25 (MPa)
    private static final double FC_BETON = 25.0;      // Resistance compression (MPa)

    // Charges standards (kN/m2)
    private static final double G_PLANCHER = 5.0;     // Charge permanente plancher BA 20cm + revetement
    private static final double G_TOITURE = 3.5;      // Toiture terrasse accessible
    private static final double Q_HABITATION = 1.5;   // Charge exploitation habitation
    private static final double Q_BUREAUX = 2.5;      // Charge exploitation bureaux
    private static final double Q_COMMERCE = 5.0;     // Charge exploitation commerce
    private static final double G_CLOISONS = 1.0;     // Cloisons legeres

    public StructuralAnalysisResponse analyze(PlanResponse plan, String buildingType) {
        if (buildingType == null) buildingType = "habitation";

        double chargeExploitation = switch (buildingType) {
            case "bureaux" -> Q_BUREAUX;
            case "commerce" -> Q_COMMERCE;
            default -> Q_HABITATION;
        };

        double chargeTotale = G_PLANCHER + chargeExploitation + G_CLOISONS;

        List<StructuralElementResultDto> elements = new ArrayList<>();

        for (var room : plan.getRooms()) {
            // La portee critique est la plus grande dimension de la piece
            double portee = Math.max(room.getWidth(), room.getHeight());
            double largeurTributaire = Math.min(room.getWidth(), room.getHeight()) / 2.0;
            double chargeLineaire = chargeTotale * largeurTributaire;

            var result = analyzeBeam(room.getName(), portee, chargeLineaire);
            elements.add(result);
        }

        boolean globalValid = elements.stream().allMatch(StructuralElementResultDto::getValid);

        String summary;
        if (globalValid) {
            summary = "Structure conforme - Toutes les portees et fleches sont dans les limites admissibles";
        } else {
            long nbProblems = elements.stream().filter(e -> !e.getValid()).count();
            summary = String.format("ATTENTION: %d element(s) necessitent revision", nbProblems);
        }

        log.info("Analyse structurelle terminee: {} elements, global_valid={}", elements.size(), globalValid);

        return StructuralAnalysisResponse.builder()
                .buildingType(buildingType)
                .chargeTotale(Math.round(chargeTotale * 100) / 100.0)
                .elements(elements)
                .globalValid(globalValid)
                .summary(summary)
                .build();
    }

    /**
     * Pre-dimensionnement d'une poutre BA (methode simplifiee).
     * Hypothese: poutre simplement appuyee, section rectangulaire.
     */
    private StructuralElementResultDto analyzeBeam(String roomName, double portee, double chargeLineaire) {
        // Hauteur recommandee: entre L/16 (appuis simples) et L/12
        double hMin = portee / 16.0;
        double hRecommandee = portee / 12.0;
        double bw = 0.25; // Largeur poutre 25cm

        // Prendre la hauteur recommandee (min 30cm)
        double h = Math.max(hRecommandee, 0.30);
        h = Math.round(h * 20) / 20.0; // Arrondir a 5cm

        // Moment flechissant max: M = q*L^2/8 (poutre simplement appuyee)
        double momentMax = chargeLineaire * portee * portee / 8.0;

        // Effort tranchant max: V = q*L/2
        double effortTranchant = chargeLineaire * portee / 2.0;

        // Fleche: f = 5*q*L^4 / (384*E*I) (en mm)
        double inertie = bw * Math.pow(h, 3) / 12.0; // m4
        double eMpa = E_BETON * 1000.0; // kN/m2
        double flecheMax = (5.0 * chargeLineaire * Math.pow(portee, 4)) / (384.0 * eMpa * inertie) * 1000.0;

        // Fleche admissible: L/500
        double flecheAdmissible = portee * 1000.0 / 500.0;

        boolean valid = flecheMax <= flecheAdmissible && portee <= 8.0;

        String message;
        if (!valid) {
            if (portee > 8.0) {
                message = String.format("Portee %.1fm excessive (max recommande 8m pour BA). Prevoir poutre pretainte ou structure metallique.", portee);
            } else {
                message = String.format("Fleche %.1fmm > admissible %.1fmm. Augmenter la section (h=%.0fcm recommande).",
                        flecheMax, flecheAdmissible, hRecommandee * 100 * 1.3);
            }
        } else {
            message = String.format("OK - Poutre %dx%.0fcm, fleche %.1fmm < %.1fmm",
                    (int) (bw * 100), h * 100, flecheMax, flecheAdmissible);
        }

        return StructuralElementResultDto.builder()
                .elementType("poutre")
                .elementId("Poutre-" + roomName)
                .portee(Math.round(portee * 100) / 100.0)
                .momentMax(Math.round(momentMax * 100) / 100.0)
                .effortTranchant(Math.round(effortTranchant * 100) / 100.0)
                .flecheMax(Math.round(flecheMax * 100) / 100.0)
                .flecheAdmissible(Math.round(flecheAdmissible * 100) / 100.0)
                .hauteurRecommandee(h)
                .valid(valid)
                .message(message)
                .build();
    }
}
