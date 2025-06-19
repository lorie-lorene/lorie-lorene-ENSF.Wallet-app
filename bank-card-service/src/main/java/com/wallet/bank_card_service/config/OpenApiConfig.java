package com.wallet.bank_card_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.openapi.prod-url:https://api.banque.com}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("URL du serveur de développement");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("URL du serveur de production");

        Contact contact = new Contact();
        contact.setEmail("dev@banque.com");
        contact.setName("Équipe Carte Bancaire");
        contact.setUrl("https://www.banque.com");

        License mitLicense = new License()
            .name("MIT License")
            .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
            .title("API Service Carte Bancaire")
            .version("1.0")
            .contact(contact)
            .description("API pour la gestion des cartes bancaires virtuelles et physiques")
            .termsOfService("https://www.banque.com/terms")
            .license(mitLicense);

        return new OpenAPI()
            .info(info)
            .servers(List.of(devServer, prodServer));
    }
}