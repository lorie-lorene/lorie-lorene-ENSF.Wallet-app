package com.wallet.money.service;

import com.wallet.money.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetraitMoneyService {

    private RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private FreemoAuthService authService;
    @Value("${freemo.api.url}")
    private String baseUrl;

    public PaymentResponse initiateWithdrawal(RetraitRequest request) {
        String token = authService.getBearerToken();
        if (token == null) {
            throw new RuntimeException("Token d'authentification FreemoPay manquant");
        }

        String url = baseUrl + "/api/v2/payment/direct-withdraw";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RetraitRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[FreemoPay] Initie un retrait : {}", request);
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, entity, PaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("[FreemoPay] Erreur lors de l'initiation du retrait", e);
            throw new RuntimeException("Erreur lors du retrait : " + e.getMessage(), e);
        }
    }
}
