package com.wallet.bank_card_service.service;

import com.wallet.bank_card_service.dto.CarteWithdrawalRequest;
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

    @Value("${money.service.url:http://localhost:8095}")
    private String moneyServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> initiateCardRecharge(String idCarte, OrangeMoneyRechargeRequest request,
            String clientId) {
        try {
            String url = moneyServiceUrl + "/api/money/card-recharge";

            // Préparer payload
            Map<String, Object> payload = Map.of(
                    "idCarte", idCarte,
                    "montant", request.getMontant(),
                    "numeroOrangeMoney", request.getNumeroOrangeMoney(),
                    "callbackUrl", buildCallbackUrl(),
                    "clientId", clientId);

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
        return "http://localhost:8095/api/v1/cartes/webhooks/money-callback";
    }

    /**
     * NOUVELLE MÉTHODE: Initier retrait depuis carte vers Mobile Money
     */
    public Map<String, Object> initiateCardWithdrawal(String idCarte, CarteWithdrawalRequest request, String clientId) {
        try {
            String url = moneyServiceUrl + "/api/withdrawals/from-card";

            // Préparer payload pour Money Service
            Map<String, Object> payload = Map.of(
                    "idCarte", idCarte,
                    "receiver", request.getNumeroTelephone(),
                    "amount", request.getMontant().doubleValue(),
                    "provider", request.getProvider(),
                    "description",
                    request.getDescription() != null ? request.getDescription() : "Retrait depuis carte " + idCarte,
                    "callbackUrl", buildWithdrawalCallbackUrl(),
                    "clientId", clientId);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-ID", clientId);
            headers.set("X-Source-Service", "carte-service");
            headers.set("X-Request-Type", "CARD_WITHDRAWAL");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("🔄 [MONEY-CLIENT] Appel retrait Money Service - Carte: {}, Montant: {}, Provider: {}",
                    idCarte, request.getMontant(), request.getProvider());

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            log.info("✅ [MONEY-CLIENT] Réponse retrait reçue du service Money");
            return response;

        } catch (Exception e) {
            log.error("❌ [MONEY-CLIENT] Erreur appel retrait service Money: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur communication service Money pour retrait", e);
        }
    }

    private String buildWithdrawalCallbackUrl() {
        return "http://localhost:8095/api/v1/cartes/webhooks/money-withdrawal-callback";
    }

}
