package com.wallet.money;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.wallet.money.controller.WebhookController;
import com.wallet.money.service.TransactionService;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void testHandlePaymentNotification_Success() throws Exception {
        // Given
        String webhookPayload = """
            {
                "reference": "freemo-ref-123",
                "status": "SUCCESS",
                "message": "Paiement confirmé"
            }
            """;

        // When & Then
        mockMvc.perform(post("/webhook/freemopay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isOk());

        verify(transactionService, times(1)).updateStatusFromWebhook("freemo-ref-123", "SUCCESS");
    }

    @Test
    void testHandlePaymentNotification_InvalidJson() throws Exception {
        // Given
        String invalidPayload = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/webhook/freemopay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isInternalServerError());

        verify(transactionService, never()).updateStatusFromWebhook(anyString(), anyString());
    }

    @Test
    void testHandlePaymentNotification_MissingReference() throws Exception {
        // Given
        String webhookPayload = """
            {
                "status": "SUCCESS",
                "message": "Paiement confirmé"
            }
            """;

        // When & Then
        mockMvc.perform(post("/webhook/freemopay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookPayload))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).updateStatusFromWebhook(anyString(), anyString());
    }
}