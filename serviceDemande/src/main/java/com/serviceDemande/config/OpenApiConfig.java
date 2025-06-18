package com.serviceDemande.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service Demande API")
                        .description("API de supervision et validation des demandes bancaires")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Équipe Supervision")
                                .email("supervision@banque.cm"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Serveur de développement"),
                        new Server()
                                .url("https://api-demande.production.com")
                                .description("Serveur de production")));
    }
}

