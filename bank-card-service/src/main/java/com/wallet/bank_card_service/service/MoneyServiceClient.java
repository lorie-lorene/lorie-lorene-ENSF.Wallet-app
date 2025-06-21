package com.wallet.bank_card_service.service;


import com.wallet.bank_card_service.dto.OrangeMoneyRechargeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class MoneyServiceClient {

    @Value("${money.service.url:http://localhost:8084}")
    private String moneyServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> initiateCardRecharge(String idCarte, OrangeMoneyRechargeRequest request, String clientId) {
        try {
            String url = moneyServiceUrl + "/api/money/card-recharge";
            
            // Préparer payload
            Map<String, Object> payload = Map.of(
                "idCarte", idCarte,
                "montant", request.getMontant(),
                "numeroOrangeMoney", request.getNumeroOrangeMoney(),
                "callbackUrl", buildCallbackUrl(),
                "clientId", clientId
            );

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-ID", clientId);
            headers.set("X-Source-Service", "carte-service");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("🔄 [MONEY-CLIENT] Appel service Money - Carte: {}, Montant: {}", idCarte, request.getMontant());

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            log.info("✅ [MONEY-CLIENT] Réponse reçue du service Money");
            return response;

        } catch (Exception e) {
            log.error("❌ [MONEY-CLIENT] Erreur appel service Money: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur communication service Money", e);
        }
    }

    private String buildCallbackUrl() {
        return "http://localhost:8083/api/v1/cartes/webhooks/money-callback";
    }
    public Map<String, Object> initiateCardDebit(String idCarte, OrangeMoneyRechargeRequest request, String clientId) {
        try {
            String url = moneyServiceUrl + "/api/money/card-recharge";
            
            // Préparer payload
            Map<String, Object> payload = Map.of(
                "idCarte", idCarte,
                "montant", request.getMontant(),
                "numeroOrangeMoney", request.getNumeroOrangeMoney(),
                "callbackUrl", buildCallbackUrl2(),
                "clientId", clientId
            );

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-ID", clientId);
            headers.set("X-Source-Service", "carte-service");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("🔄 [MONEY-CLIENT] Appel service Money - Carte: {}, Montant: {}", idCarte, request.getMontant());

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            log.info("✅ [MONEY-CLIENT] Réponse reçue du service Money");
            return response;

        } catch (Exception e) {
            log.error("❌ [MONEY-CLIENT] Erreur appel service Money: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur communication service Money", e);
        }
    }

    private String buildCallbackUrl2() {
        return "http://localhost:8083/api/v1/cartes/webhooks/money-callback";
    }
}
