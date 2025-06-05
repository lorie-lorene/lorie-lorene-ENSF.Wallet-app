package com.wallet.money;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wallet.money.entity.Transaction;
import com.wallet.money.repository.TransactionRepository;
import com.wallet.money.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockTransaction = new Transaction();
        mockTransaction.setId("mock-id-123");
        mockTransaction.setClientId("client123");
        mockTransaction.setExternalId("DEP_client123_1635789456789");
        mockTransaction.setPhoneNumber("237654123456");
        mockTransaction.setAmount(BigDecimal.valueOf(1000));
        mockTransaction.setType("DEPOSIT");
        mockTransaction.setStatus("PENDING");
        mockTransaction.setCreatedAt(LocalDateTime.now());
        mockTransaction.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreatePendingDeposit_Success() {
        // Given
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        // When
        Transaction result = transactionService.createPendingDeposit("client123", "237654123456", 1000.0);

        // Then
        assertNotNull(result);
        assertEquals("client123", result.getClientId());
        assertEquals("237654123456", result.getPhoneNumber());
        assertEquals(BigDecimal.valueOf(1000), result.getAmount());
        assertEquals("DEPOSIT", result.getType());
        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getExternalId().startsWith("DEP_client123_"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateFreemoReference_Success() {
        // Given
        String transactionId = "mock-id-123";
        String freemoReference = "freemo-ref-456";
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        // When
        transactionService.updateFreemoReference(transactionId, freemoReference);

        // Then
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateFreemoReference_TransactionNotFound() {
        // Given
        String transactionId = "non-existent-id";
        String freemoReference = "freemo-ref-456";
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // When
        transactionService.updateFreemoReference(transactionId, freemoReference);

        // Then
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testUpdateStatusFromWebhook_Success() {
        // Given
        String freemoReference = "freemo-ref-456";
        mockTransaction.setFreemoReference(freemoReference);
        when(transactionRepository.findByFreemoReference(freemoReference)).thenReturn(Optional.of(mockTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        // When
        transactionService.updateStatusFromWebhook(freemoReference, "SUCCESS");

        // Then
        verify(transactionRepository, times(1)).findByFreemoReference(freemoReference);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateStatusFromWebhook_TransactionNotFound() {
        // Given
        String freemoReference = "non-existent-ref";
        when(transactionRepository.findByFreemoReference(freemoReference)).thenReturn(Optional.empty());

        // When
        transactionService.updateStatusFromWebhook(freemoReference, "SUCCESS");

        // Then
        verify(transactionRepository, times(1)).findByFreemoReference(freemoReference);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testUpdateStatusFromWebhook_AlreadyProcessed() {
        // Given
        String freemoReference = "freemo-ref-456";
        mockTransaction.setFreemoReference(freemoReference);
        mockTransaction.setStatus("SUCCESS"); // Déjà traité
        when(transactionRepository.findByFreemoReference(freemoReference)).thenReturn(Optional.of(mockTransaction));

        // When
        transactionService.updateStatusFromWebhook(freemoReference, "SUCCESS");

        // Then
        verify(transactionRepository, times(1)).findByFreemoReference(freemoReference);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testFindByExternalId_Found() {
        // Given
        String externalId = "DEP_client123_1635789456789";
        when(transactionRepository.findByExternalId(externalId)).thenReturn(Optional.of(mockTransaction));

        // When
        Transaction result = transactionService.findByExternalId(externalId);

        // Then
        assertNotNull(result);
        assertEquals(externalId, result.getExternalId());
        verify(transactionRepository, times(1)).findByExternalId(externalId);
    }

    @Test
    void testFindByExternalId_NotFound() {
        // Given
        String externalId = "non-existent-id";
        when(transactionRepository.findByExternalId(externalId)).thenReturn(Optional.empty());

        // When
        Transaction result = transactionService.findByExternalId(externalId);

        // Then
        assertNull(result);
        verify(transactionRepository, times(1)).findByExternalId(externalId);
    }
}
