package tn.archigen.archigen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI archigenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ArchiGenTN API")
                        .description("API de generation de plans architecturaux pour le marche tunisien. "
                                + "Genere des plans, exporte en DXF (AutoCAD) et effectue des analyses structurelles.")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("ArchiGenTN")
                                .email("contact@archigentn.tn"))
                        .license(new License()
                                .name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Developpement local")
                ));
    }
}
