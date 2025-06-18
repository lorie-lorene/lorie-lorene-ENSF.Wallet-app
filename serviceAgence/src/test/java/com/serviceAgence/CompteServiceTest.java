package com.serviceAgence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.serviceAgence.dto.AccountCreationRequest;
import com.serviceAgence.dto.AccountCreationResult;
import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.enums.CompteType;
import com.serviceAgence.exception.CompteException;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.repository.CompteRepository;
import com.serviceAgence.services.CompteService;
import com.serviceAgence.services.NotificationService;
import com.serviceAgence.utils.AccountNumberGenerator;

@ExtendWith(MockitoExtension.class)
class CompteServiceTest {

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CompteService compteService;

    private AccountCreationRequest validRequest;
    private CompteUser testCompte;

    @BeforeEach
    void setUp() {
        validRequest = new AccountCreationRequest();
        validRequest.setIdClient("CLIENT123");
        validRequest.setIdAgence("AGENCE001");

        testCompte = new CompteUser();
        testCompte.setId("COMPTE_ID_123");
        testCompte.setNumeroCompte(123456789L);
        testCompte.setIdClient("CLIENT123");
        testCompte.setIdAgence("AGENCE001");
        testCompte.setSolde(new BigDecimal("5000"));
        testCompte.setStatus(CompteStatus.ACTIVE);
        testCompte.setType(CompteType.STANDARD);
        testCompte.setCreatedAt(LocalDateTime.now());
        testCompte.setBlocked(false);
    }

    @Test
    void testCreateAccount_Success() {
        // Given
        when(compteRepository.existsByIdClientAndIdAgence("CLIENT123", "AGENCE001"))
                .thenReturn(false);
        when(accountNumberGenerator.generateAccountNumber("CLIENT123", "AGENCE001"))
                .thenReturn(123456789L);
        when(compteRepository.save(any(CompteUser.class)))
                .thenReturn(testCompte);

        // When
        AccountCreationResult result = compteService.createAccount(validRequest);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(123456789L, result.getNumeroCompte());
        assertEquals("CLIENT123", result.getIdClient());

        verify(compteRepository).existsByIdClientAndIdAgence("CLIENT123", "AGENCE001");
        verify(accountNumberGenerator).generateAccountNumber("CLIENT123", "AGENCE001");
        verify(compteRepository).save(any(CompteUser.class));

        System.out.println("✅ Test création compte réussi");
    }

    @Test
    void testCreateAccount_AccountAlreadyExists() {
        // Given
        when(compteRepository.existsByIdClientAndIdAgence("CLIENT123", "AGENCE001"))
                .thenReturn(true);

        // When
        AccountCreationResult result = compteService.createAccount(validRequest);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("COMPTE_DEJA_EXISTANT", result.getErrorCode());
        assertTrue(result.getMessage().contains("existe déjà"));

        verify(compteRepository).existsByIdClientAndIdAgence("CLIENT123", "AGENCE001");
        verify(compteRepository, never()).save(any(CompteUser.class));
        verify(accountNumberGenerator, never()).generateAccountNumber(any(), any());

        System.out.println("✅ Test compte existant réussi");
    }

    @Test
    void testActivateAccount_Success() {
        // Given
        testCompte.setStatus(CompteStatus.PENDING);
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.of(testCompte));
        when(compteRepository.save(any(CompteUser.class)))
                .thenReturn(testCompte);

        // When
        assertDoesNotThrow(() -> compteService.activateAccount("123456789", "ADMIN_USER"));

        // Then
        assertEquals(CompteStatus.ACTIVE, testCompte.getStatus());
        assertNotNull(testCompte.getActivatedAt());
        assertEquals("ADMIN_USER", testCompte.getActivatedBy());

        verify(compteRepository).findByNumeroCompte(123456789L);
        verify(compteRepository).save(testCompte);
        verify(notificationService).sendAccountActivationNotification(testCompte);

        System.out.println("✅ Test activation compte réussi");
    }

    @Test
    void testActivateAccount_AlreadyActive() {
        // Given
        testCompte.setStatus(CompteStatus.ACTIVE);
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.of(testCompte));

        // When & Then
        CompteException exception = assertThrows(CompteException.class, () -> {
            compteService.activateAccount("123456789", "ADMIN_USER");
        });

        assertEquals("COMPTE_DEJA_ACTIF", exception.getErrorCode());
        verify(compteRepository, never()).save(any());

        System.out.println("✅ Test compte déjà actif réussi");
    }

    @Test
    void testSuspendAccount_Success() {
        // Given
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.of(testCompte));
        when(compteRepository.save(any(CompteUser.class)))
                .thenReturn(testCompte);

        // When
        assertDoesNotThrow(() -> compteService.suspendAccount("123456789", "Activité suspecte", "ADMIN_USER"));

        // Then
        assertEquals(CompteStatus.SUSPENDED, testCompte.getStatus());
        assertTrue(testCompte.getBlocked());
        assertEquals("Activité suspecte", testCompte.getBlockedReason());
        assertEquals("ADMIN_USER", testCompte.getBlockedBy());
        assertNotNull(testCompte.getBlockedAt());

        verify(compteRepository).findByNumeroCompte(123456789L);
        verify(compteRepository).save(testCompte);
        verify(notificationService).sendAccountSuspensionNotification(testCompte, "Activité suspecte");

        System.out.println("✅ Test suspension compte réussi");
    }

    @Test
    void testGetAccountBalance_Success() {
        // Given
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.of(testCompte));

        // When
        BigDecimal balance = compteService.getAccountBalance("123456789");

        // Then
        assertEquals(new BigDecimal("5000"), balance);
        verify(compteRepository).findByNumeroCompte(123456789L);

        System.out.println("✅ Test récupération solde réussi");
    }

    @Test
    void testGetAccountBalance_CompteNotFound() {
        // Given
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.empty());

        // When & Then
        CompteException exception = assertThrows(CompteException.class, () -> {
            compteService.getAccountBalance("123456789");
        });

        assertEquals("COMPTE_INTROUVABLE", exception.getErrorCode());

        System.out.println("✅ Test compte introuvable réussi");
    }

    @Test
    void testGetClientAccounts() {
        // Given
        List<CompteUser> comptes = Arrays.asList(testCompte);
        when(compteRepository.findByIdClientOrderByCreatedAtDesc("CLIENT123"))
                .thenReturn(comptes);

        // When
        List<CompteUser> result = compteService.getClientAccounts("CLIENT123");

        // Then
        assertEquals(1, result.size());
        assertEquals(testCompte, result.get(0));
        verify(compteRepository).findByIdClientOrderByCreatedAtDesc("CLIENT123");

        System.out.println("✅ Test récupération comptes client réussi");
    }

    @Test
    void testGetAccountStatistics() {
        // Given
        // ✅ CORRIGER LES MOCKS pour correspondre à votre service réel
        when(compteRepository.countByIdAgence("AGENCE001"))
                .thenReturn(10L); // ✅ long, pas BigDecimal !
        when(compteRepository.countByIdAgenceAndStatus("AGENCE001", CompteStatus.ACTIVE))
                .thenReturn(8L);
        when(compteRepository.countByIdAgenceAndStatus("AGENCE001", CompteStatus.SUSPENDED))
                .thenReturn(1L);
        when(compteRepository.countByIdAgenceAndStatus("AGENCE001", CompteStatus.BLOCKED))
                .thenReturn(1L);

        // ✅ MOCK pour findByIdAgence (pour le calcul des soldes)
        CompteUser compte1 = new CompteUser();
        compte1.setSolde(new BigDecimal("20000"));
        CompteUser compte2 = new CompteUser();
        compte2.setSolde(new BigDecimal("30000"));
        List<CompteUser> comptes = Arrays.asList(compte1, compte2);

        when(compteRepository.findByIdAgence("AGENCE001"))
                .thenReturn(comptes);

        // When
        Map<String, Object> stats = compteService.getAccountStatistics("AGENCE001");

        // Then
        assertEquals(10L, stats.get("totalComptes"));
        assertEquals(8L, stats.get("comptesActifs"));
        assertEquals(1L, stats.get("comptesSuspendus"));
        assertEquals(1L, stats.get("comptesBloqués"));
        assertEquals(new BigDecimal("50000"), stats.get("totalSoldes")); // 20000 + 30000

        // ✅ Vérifier les appels
        verify(compteRepository).countByIdAgence("AGENCE001");
        verify(compteRepository).countByIdAgenceAndStatus("AGENCE001", CompteStatus.ACTIVE);
        verify(compteRepository).countByIdAgenceAndStatus("AGENCE001", CompteStatus.SUSPENDED);
        verify(compteRepository).countByIdAgenceAndStatus("AGENCE001", CompteStatus.BLOCKED);
        verify(compteRepository).findByIdAgence("AGENCE001");

        System.out.println("✅ Test statistiques compte réussi");
        System.out.println("📊 Stats: " + stats);
    }

    @Test
    void testUpdateAccountLimits() {
        // Given
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.of(testCompte));
        when(compteRepository.save(any(CompteUser.class)))
                .thenReturn(testCompte);

        BigDecimal newDailyWithdrawal = new BigDecimal("2000000");
        BigDecimal newDailyTransfer = new BigDecimal("3000000");
        BigDecimal newMonthlyOperations = new BigDecimal("15000000");

        // When
        assertDoesNotThrow(() -> compteService.updateAccountLimits(
                "123456789", newDailyWithdrawal, newDailyTransfer, newMonthlyOperations));

        // Then
        assertEquals(newDailyWithdrawal, testCompte.getLimiteDailyWithdrawal());
        assertEquals(newDailyTransfer, testCompte.getLimiteDailyTransfer());
        assertEquals(newMonthlyOperations, testCompte.getLimiteMonthlyOperations());

        verify(compteRepository).save(testCompte);

        System.out.println("✅ Test mise à jour limites réussi");
    }

    @Test
    void testGetAccountDetails_Success() {
        // Given
        when(compteRepository.findByNumeroCompte(123456789L))
                .thenReturn(Optional.of(testCompte));

        // When
        CompteUser result = compteService.getAccountDetails("123456789");

        // Then
        assertNotNull(result);
        assertEquals(testCompte.getNumeroCompte(), result.getNumeroCompte());
        assertEquals(testCompte.getIdClient(), result.getIdClient());
        assertEquals(testCompte.getSolde(), result.getSolde());

        verify(compteRepository).findByNumeroCompte(123456789L);

        System.out.println("✅ Test récupération détails compte réussi");
    }
}