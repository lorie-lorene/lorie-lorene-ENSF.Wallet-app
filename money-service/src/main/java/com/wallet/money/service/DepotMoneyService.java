package com.wallet.money.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.entity.Transaction;
import com.wallet.money.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j

public class DepotMoneyService {

    @Value("${freemo.api.url}")
    private String baseUrl;

    @Value("${freemo.api.appKey}")
    private String apiToken;

    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private FreemoAuthService authService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionService transactionService;

    public PaymentResponse createPayment2(PaymentRequest paymentRequest) {
        String token = authService.getBearerToken();

        log.info("Token reçu : {}", token);

        if (token == null) {
            throw new RuntimeException("Impossible d'obtenir un token FreemoPay");
        }

        String url = baseUrl + "/api/v2/payment";
        log.info("Appel de l'URL : {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

        try {
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, entity, PaymentResponse.class);
            log.info("Réponse reçue : {}", response);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erreur lors de la création du paiement : ", e);
            throw new RuntimeException("Erreur lors de la création du paiement : " + e.getMessage(), e);
        }
    }

    public PaymentResponse getPaymentStatus(String reference) {
        String token = authService.getBearerToken(); // Utiliser le token JWT
        String url = baseUrl + "/api/v2/payment/" + reference;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("[FreemoPay] Vérification statut paiement: {}", reference);
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    PaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("[FreemoPay] Erreur lors de la récupération du statut du paiement", e);
            throw new RuntimeException("Impossible de récupérer le statut du paiement");
        }
    }
    // AJOUTER cette méthode
@Scheduled(fixedRate = 120000) // Toutes les 2 minutes
public void checkPendingPayments() {
    List<Transaction> pending = transactionRepository.findByStatus("PENDING");
    
    for (Transaction t : pending) {
        try {
            PaymentResponse status = getPaymentStatus(t.getFreemoReference());
            
            if ("FAILED".equals(status.getStatus())) {
                transactionService.updateStatusFromWebhook(
                    t.getFreemoReference(), "FAILED");
            }
        } catch (Exception e) {
            log.warn("Erreur vérification statut: {}", e.getMessage());
        }
    }
}

}
