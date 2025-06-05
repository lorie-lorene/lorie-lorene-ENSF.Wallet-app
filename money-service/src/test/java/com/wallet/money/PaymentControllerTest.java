package com.wallet.money;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.money.controller.PaymentController;
import com.wallet.money.entity.PaymentRequest;
import com.wallet.money.entity.PaymentResponse;
import com.wallet.money.entity.Transaction;
import com.wallet.money.service.DepotMoneyService;
import com.wallet.money.service.TransactionService;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepotMoneyService depotMoneyService;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setPayer("237654123456");
        paymentRequest.setAmount(1000.0);
        paymentRequest.setDescription("Test dépôt");
        paymentRequest.setCallback("https://webhook.test.com");

        paymentResponse = new PaymentResponse();
        paymentResponse.setReference("freemo-ref-123");
        paymentResponse.setStatus("PENDING");
        paymentResponse.setMessage("SMS envoyé");

        mockTransaction = new Transaction();
        mockTransaction.setId("trans-id-123");
        mockTransaction.setClientId("client123");
        mockTransaction.setExternalId("DEP_client123_123456789");
        mockTransaction.setPhoneNumber("237654123456");
        mockTransaction.setAmount(BigDecimal.valueOf(1000));
        mockTransaction.setType("DEPOSIT");
        mockTransaction.setStatus("PENDING");
        mockTransaction.setCreatedAt(LocalDateTime.now());
        mockTransaction.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testMakePayment_Success() throws Exception {
        // Given
        when(transactionService.createPendingDeposit(anyString(), anyString(), anyDouble()))
                .thenReturn(mockTransaction);
        when(depotMoneyService.createPayment2(any(PaymentRequest.class)))
                .thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-ID", "client123")
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("freemo-ref-123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("SMS envoyé"));

        verify(transactionService, times(1)).createPendingDeposit("client123", "237654123456", 1000.0);
        verify(depotMoneyService, times(1)).createPayment2(any(PaymentRequest.class));
        verify(transactionService, times(1)).updateFreemoReference("trans-id-123", "freemo-ref-123");
    }

    @Test
    void testMakePayment_WithoutClientId() throws Exception {
        // Given
        when(transactionService.createPendingDeposit(anyString(), anyString(), anyDouble()))
                .thenReturn(mockTransaction);
        when(depotMoneyService.createPayment2(any(PaymentRequest.class)))
                .thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());

        verify(transactionService, times(1)).createPendingDeposit("unknown", "237654123456", 1000.0);
    }

    @Test
    void testMakePayment_ServiceException() throws Exception {
        // Given
        when(transactionService.createPendingDeposit(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-ID", "client123")
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, times(1)).createPendingDeposit("client123", "237654123456", 1000.0);
        verify(depotMoneyService, never()).createPayment2(any());
    }

    @Test
    void testCheckPayment_Success() throws Exception {
        // Given
        String reference = "freemo-ref-123";
        when(depotMoneyService.getPaymentStatus(reference)).thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(get("/api/deposit/{reference}", reference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("freemo-ref-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(depotMoneyService, times(1)).getPaymentStatus(reference);
    }
}