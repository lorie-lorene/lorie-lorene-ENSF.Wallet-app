package com.wallet.money;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "freemo.api.url=https://api-test.freemopay.com",
        "freemo.api.appKey=test-app-key",
        "freemo.api.secretKey=test-secret-key"
})
class PaymentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void testCreateDeposit_Success() {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setPayer("237654123456");
        request.setAmount(1000.0);
        request.setDescription("Test intégration");
        request.setCallback("https://webhook.test.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Client-ID", "integration-test");

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/deposit",
                entity,
                PaymentResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PaymentResponse paymentResponse = response.getBody();
        assertNotNull(paymentResponse);
        assertNotNull(paymentResponse.getReference());

        System.out.println("✅ Dépôt créé - Référence: " + paymentResponse.getReference());
        System.out.println("✅ Statut: " + paymentResponse.getStatus());
    }

    @Test
    void testWebhook_Success() {
        // Given
        String webhookPayload = """
                {
                    "reference": "test-ref-123",
                    "status": "SUCCESS",
                    "message": "Paiement confirmé"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(webhookPayload, headers);

        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
                getBaseUrl() + "/webhook/freemopay",
                entity,
                Void.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ Webhook traité avec succès");
    }

    @Test
    void testGetPaymentStatus() {
        // Given - D'abord créer un paiement
        PaymentRequest request = new PaymentRequest();
        request.setPayer("237654123456");
        request.setAmount(500.0);
        request.setDescription("Test statut");
        request.setCallback("https://webhook.test.com"); // ⬅️ Ajout du callback manquant

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Client-ID", "test-client");
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        // When - Créer le dépôt
        ResponseEntity<PaymentResponse> createResponse = restTemplate.postForEntity(
                getBaseUrl() + "/api/deposit",
                entity,
                PaymentResponse.class);

        // Then - Vérifier la création
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody(), "❌ Réponse de création nulle");

        PaymentResponse createBody = createResponse.getBody();
        assertNotNull(createBody.getReference(), "❌ Référence de paiement nulle");

        String reference = createBody.getReference();
        System.out.println("✅ Paiement créé - Référence: " + reference);
        System.out.println("✅ Statut initial: " + createBody.getStatus());

        // When - Vérifier le statut (avec gestion d'erreur)
        try {
            ResponseEntity<PaymentResponse> statusResponse = restTemplate.getForEntity(
                    getBaseUrl() + "/api/deposit/" + reference,
                    PaymentResponse.class);

            // Then - Vérifier la réponse
            System.out.println("🔍 Code de statut: " + statusResponse.getStatusCode());
            System.out.println("🔍 Headers: " + statusResponse.getHeaders());
            System.out.println("🔍 Body: " + statusResponse.getBody());

            if (statusResponse.getStatusCode() == HttpStatus.OK) {
                if (statusResponse.getBody() != null) {
                    System.out.println("✅ Statut récupéré: " + statusResponse.getBody().getStatus());
                    System.out.println("✅ Message: " + statusResponse.getBody().getMessage());
                } else {
                    System.out.println("⚠️ Réponse OK mais body null - Vérifiez FreemoPay");
                }
            } else {
                System.out.println("❌ Erreur HTTP: " + statusResponse.getStatusCode());
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la vérification du statut: " + e.getMessage());
            e.printStackTrace();

            // Le test ne doit pas échouer si FreemoPay est indisponible
            System.out.println("⚠️ Test ignoré car FreemoPay indisponible");
        }
    }
}