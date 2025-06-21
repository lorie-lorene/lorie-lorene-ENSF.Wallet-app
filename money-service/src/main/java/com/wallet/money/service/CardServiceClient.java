package com.wallet.money.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.wallet.money.carteclient.CallbackPayload;

@Service
@Slf4j
public class CardServiceClient {

    @Value("${card.service.url:http://localhost:8083}")
    private String cardServiceUrl;

    @Value("${card.service.auth.username:client}")
    private String username;

    @Value("${card.service.auth.password:password}")
    private String password;

    private final RestTemplate restTemplate;

    public CardServiceClient() {
        this.restTemplate = new RestTemplate();
        // Ajouter l'authentification HTTP Basic
        this.restTemplate.getInterceptors().add(
            (request, body, execution) -> {
                String auth = username + ":" + password;
                byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
                String authHeader = "Basic " + new String(encodedAuth);
                request.getHeaders().set("Authorization", authHeader);
                return execution.execute(request, body);
            }
        );
    }

    /**
     * Envoyer callback au service Carte
     */
    public void sendRechargeCallback(String callbackUrl, CallbackPayload payload) {
        try {
            log.info("🔄 [CALLBACK] Envoi notification au service Carte - RequestId: {}, Status: {}", 
                    payload.getRequestId(), payload.getStatus());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Source-Service", "money-service");

            HttpEntity<CallbackPayload> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForObject(callbackUrl, entity, Void.class);

            log.info("✅ [CALLBACK] Notification envoyée avec succès");

        } catch (Exception e) {
            log.error("❌ [CALLBACK] Erreur envoi notification: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur notification service Carte", e);
        }
    }
    
}