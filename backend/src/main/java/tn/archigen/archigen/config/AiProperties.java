package tn.archigen.archigen.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "archigentn.ai")
public class AiProperties {

    /** Cle API Claude (Anthropic). Si absente, fallback sur l'algo. */
    private String apiKey = "";

    /** Modele Claude a utiliser. */
    private String model = "claude-sonnet-4-5-20250929";

    /** URL de base de l'API Anthropic. */
    private String apiUrl = "https://api.anthropic.com/v1/messages";

    /** Nombre max de tokens en reponse. */
    private int maxTokens = 4096;

    /** Temperature (0.0 = deterministe, 1.0 = creatif). */
    private double temperature = 0.3;

    /** Timeout en secondes pour l'appel API. */
    private int timeout = 60;
}
