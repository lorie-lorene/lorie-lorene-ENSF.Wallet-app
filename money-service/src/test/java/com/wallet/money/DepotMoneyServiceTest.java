package com.wallet.money;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.service.DepotMoneyService;
import com.wallet.money.service.FreemoAuthService;

@ExtendWith(MockitoExtension.class)
class DepotMoneyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FreemoAuthService authService;

    @InjectMocks
    private DepotMoneyService depotMoneyService;

    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        // Configuration des propriétés
        ReflectionTestUtils.setField(depotMoneyService, "baseUrl", "https://api.freemopay.com");
        ReflectionTestUtils.setField(depotMoneyService, "apiToken", "test-token");
        ReflectionTestUtils.setField(depotMoneyService, "restTemplate", restTemplate);

        // Données de test
        paymentRequest = new PaymentRequest();
        paymentRequest.setPayer("237654123456");
        paymentRequest.setAmount(1000.0);
        paymentRequest.setExternalId("DEP_client123_123456789");
        paymentRequest.setDescription("Test dépôt");
        paymentRequest.setCallback("https://webhook.test.com");

        paymentResponse = new PaymentResponse();
        paymentResponse.setReference("freemo-ref-123");
        paymentResponse.setStatus("PENDING");
        paymentResponse.setMessage("SMS envoyé");
    }

    @Test
    void testCreatePayment2_Success() {
        // Given
        when(authService.getBearerToken()).thenReturn("valid-token");
        when(restTemplate.postForEntity(anyString(), any(), eq(PaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        // When
        PaymentResponse result = depotMoneyService.createPayment2(paymentRequest);

        // Then
        assertNotNull(result);
        assertEquals("freemo-ref-123", result.getReference());
        assertEquals("PENDING", result.getStatus());
        assertEquals("SMS envoyé", result.getMessage());

        verify(authService, times(1)).getBearerToken();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(PaymentResponse.class));
    }

    @Test
    void testCreatePayment2_TokenNull() {
        // Given
        when(authService.getBearerToken()).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            depotMoneyService.createPayment2(paymentRequest);
        });

        assertEquals("Impossible d'obtenir un token FreemoPay", exception.getMessage());
        verify(authService, times(1)).getBearerToken();
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(PaymentResponse.class));
    }

    @Test
    void testCreatePayment2_RestTemplateException() {
        // Given
        when(authService.getBearerToken()).thenReturn("valid-token");
        when(restTemplate.postForEntity(anyString(), any(), eq(PaymentResponse.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            depotMoneyService.createPayment2(paymentRequest);
        });

        assertTrue(exception.getMessage().contains("Erreur lors de la création du paiement"));
        verify(authService, times(1)).getBearerToken();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(PaymentResponse.class));
    }

    @Test
    void testGetPaymentStatus_Success() {
        // Given
        String reference = "freemo-ref-123";
        when(authService.getBearerToken()).thenReturn("valid-token");
        when(restTemplate.exchange(anyString(), any(), any(), eq(PaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(paymentResponse, HttpStatus.OK));

        // When
        PaymentResponse result = depotMoneyService.getPaymentStatus(reference);

        // Then
        assertNotNull(result);
        assertEquals("freemo-ref-123", result.getReference());

        verify(authService, times(1)).getBearerToken();
        verify(restTemplate, times(1)).exchange(anyString(), any(), any(), eq(PaymentResponse.class));
    }
}
