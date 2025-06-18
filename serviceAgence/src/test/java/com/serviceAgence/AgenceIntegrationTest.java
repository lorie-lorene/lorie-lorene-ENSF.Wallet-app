package com.serviceAgence;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.serviceAgence.dto.AccountCreationRequest;
import com.serviceAgence.dto.AccountCreationResult;
import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.repository.CompteRepository;
import com.serviceAgence.services.AgenceService;
import com.serviceAgence.services.CompteService;

@SpringBootTest(classes = ServiceAgenceApplication.class)
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/agence_test",
        "spring.rabbitmq.host=localhost",
        "logging.level.com.serviceAgence=DEBUG"
})
// Supprimé @Transactional car MongoDB ne supporte pas les transactions
// traditionnelles
class AgenceIntegrationTest {

    @Autowired
    private AgenceService agenceService;

    @Autowired
    private CompteService compteService;

    @Autowired
    private CompteRepository compteRepository;

    private CompteUser testCompte;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de test manuellement
        cleanupTestData();

        // Créer un compte de test
        AccountCreationRequest request = new AccountCreationRequest();
        request.setIdClient("CLIENT_TEST");
        request.setIdAgence("AGENCE_TEST");

        AccountCreationResult result = compteService.createAccount(request);
        assertTrue(result.isSuccess());

        testCompte = compteService.getAccountDetails(result.getNumeroCompte().toString());
        testCompte.setSolde(new BigDecimal("10000"));
        testCompte.setStatus(CompteStatus.ACTIVE);
        compteRepository.save(testCompte);
    }

    @AfterEach
    void tearDown() {
        // Nettoyer après chaque test
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            compteRepository.deleteAll();
        } catch (Exception e) {
            // Ignorer les erreurs de nettoyage
        }
    }

    @Test
    void testCreateAccount_Success() {
        AccountCreationRequest request = new AccountCreationRequest();
        request.setIdClient("NEW_CLIENT");
        request.setIdAgence("AGENCE_TEST");

        AccountCreationResult result = compteService.createAccount(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.getNumeroCompte());
        assertEquals("NEW_CLIENT", result.getIdClient());
    }

    @Test
    void testCreateAccount_Duplicate() {
        // Tenter de créer un compte en double
        AccountCreationRequest request = new AccountCreationRequest();
        request.setIdClient("CLIENT_TEST");
        request.setIdAgence("AGENCE_TEST");

        AccountCreationResult result = compteService.createAccount(request);

        assertFalse(result.isSuccess());
        assertEquals("COMPTE_DEJA_EXISTANT", result.getErrorCode());
    }

    @Test
    void testGetAccountDetails_Success() {
        CompteUser compte = compteService.getAccountDetails(testCompte.getNumeroCompte().toString());

        assertNotNull(compte);
        assertEquals(testCompte.getNumeroCompte(), compte.getNumeroCompte());
        assertEquals("CLIENT_TEST", compte.getIdClient());
        assertEquals(CompteStatus.ACTIVE, compte.getStatus());
        assertEquals(new BigDecimal("10000"), compte.getSolde());
    }

    @Test
    void testGetAccountDetails_NotFound() {
        assertThrows(Exception.class, () -> {
            compteService.getAccountDetails("999999999");
        });
    }

    @Test
    void testActivateAccount_Success() {
        // Changer le statut pour tester l'activation
        testCompte.setStatus(CompteStatus.PENDING);
        compteRepository.save(testCompte);

        assertDoesNotThrow(() -> {
            compteService.activateAccount(testCompte.getNumeroCompte().toString(), "ADMIN_TEST");
        });

        // Vérifier l'activation
        CompteUser activatedCompte = compteService.getAccountDetails(testCompte.getNumeroCompte().toString());
        assertEquals(CompteStatus.ACTIVE, activatedCompte.getStatus());
        assertEquals("ADMIN_TEST", activatedCompte.getActivatedBy());
        assertNotNull(activatedCompte.getActivatedAt());
    }

    @Test
    void testActivateAccount_AlreadyActive() {
        // Le compte est déjà actif
        assertThrows(Exception.class, () -> {
            compteService.activateAccount(testCompte.getNumeroCompte().toString(), "ADMIN_TEST");
        });
    }

    @Test
    void testSuspendAccount_Success() {
        assertDoesNotThrow(() -> {
            compteService.suspendAccount(testCompte.getNumeroCompte().toString(),
                    "Test suspension", "ADMIN_TEST");
        });

        // Vérifier la suspension
        CompteUser suspendedCompte = compteService.getAccountDetails(testCompte.getNumeroCompte().toString());
        assertEquals(CompteStatus.SUSPENDED, suspendedCompte.getStatus());
        assertTrue(suspendedCompte.getBlocked());
        assertEquals("Test suspension", suspendedCompte.getBlockedReason());
        assertEquals("ADMIN_TEST", suspendedCompte.getBlockedBy());
        assertNotNull(suspendedCompte.getBlockedAt());
    }

    @Test
    void testBlockAccount_Success() {
        assertDoesNotThrow(() -> {
            compteService.blockAccount(testCompte.getNumeroCompte().toString(),
                    "Test blocage", "ADMIN_TEST");
        });

        // Vérifier le blocage
        CompteUser blockedCompte = compteService.getAccountDetails(testCompte.getNumeroCompte().toString());
        assertEquals(CompteStatus.BLOCKED, blockedCompte.getStatus());
        assertTrue(blockedCompte.getBlocked());
        assertEquals("Test blocage", blockedCompte.getBlockedReason());
    }

    @Test
    void testGetAccountBalance() {
        BigDecimal balance = compteService.getAccountBalance(testCompte.getNumeroCompte().toString());
        assertEquals(new BigDecimal("10000"), balance);
    }

    @Test
    void testGetClientAccounts() {
        List<CompteUser> comptes = compteService.getClientAccounts("CLIENT_TEST");

        assertNotNull(comptes);
        assertEquals(1, comptes.size());
        assertEquals(testCompte.getNumeroCompte(), comptes.get(0).getNumeroCompte());
    }

    @Test
    void testGetAgenceAccounts() {
        List<CompteUser> comptes = compteService.getAgenceAccounts("AGENCE_TEST", 10);

        assertNotNull(comptes);
        assertEquals(1, comptes.size());
        assertEquals(testCompte.getNumeroCompte(), comptes.get(0).getNumeroCompte());
    }

    @Test
    void testUpdateAccountLimits() {
        BigDecimal newDailyWithdrawal = new BigDecimal("2000000");
        BigDecimal newDailyTransfer = new BigDecimal("3000000");
        BigDecimal newMonthlyOperations = new BigDecimal("15000000");

        assertDoesNotThrow(() -> {
            compteService.updateAccountLimits(
                    testCompte.getNumeroCompte().toString(),
                    newDailyWithdrawal,
                    newDailyTransfer,
                    newMonthlyOperations);
        });

        // Vérifier les limites
        CompteUser updatedCompte = compteService.getAccountDetails(testCompte.getNumeroCompte().toString());
        assertEquals(newDailyWithdrawal, updatedCompte.getLimiteDailyWithdrawal());
        assertEquals(newDailyTransfer, updatedCompte.getLimiteDailyTransfer());
        assertEquals(newMonthlyOperations, updatedCompte.getLimiteMonthlyOperations());
    }

    @Test
    void testAccountStatistics() {
        // Créer quelques comptes supplémentaires pour les statistiques
        AccountCreationRequest request2 = new AccountCreationRequest();
        request2.setIdClient("CLIENT_2");
        request2.setIdAgence("AGENCE_TEST");
        compteService.createAccount(request2);

        AccountCreationRequest request3 = new AccountCreationRequest();
        request3.setIdClient("CLIENT_3");
        request3.setIdAgence("AGENCE_TEST");
        var result3 = compteService.createAccount(request3);

        // Suspendre un compte
        compteService.suspendAccount(result3.getNumeroCompte().toString(), "Test", "ADMIN");

        // Vérifier les statistiques
        var stats = compteService.getAccountStatistics("AGENCE_TEST");

        assertEquals(3L, stats.get("totalComptes"));
        assertEquals(2L, stats.get("comptesActifs")); // 2 actifs
        assertEquals(1L, stats.get("comptesSuspendus")); // 1 suspendu
        assertEquals(0L, stats.get("comptesBloqués")); // 0 bloqué

        BigDecimal totalSoldes = (BigDecimal) stats.get("totalSoldes");
        assertTrue(totalSoldes.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testMultipleAccountsPerClient() {
        // Un client ne peut avoir qu'un compte par agence
        AccountCreationRequest request = new AccountCreationRequest();
        request.setIdClient("CLIENT_TEST");
        request.setIdAgence("AGENCE_TEST");

        AccountCreationResult result = compteService.createAccount(request);
        assertFalse(result.isSuccess());
        assertEquals("COMPTE_DEJA_EXISTANT", result.getErrorCode());
    }

    @Test
    void testAccountNumberValidation() {
        // Test avec numéro de compte invalide
        assertThrows(Exception.class, () -> {
            compteService.getAccountDetails("INVALID_NUMBER");
        });

        assertThrows(Exception.class, () -> {
            compteService.getAccountDetails("");
        });

        assertThrows(Exception.class, () -> {
            compteService.getAccountDetails(null);
        });
    }

    @Test
    void testFindAccountByNumber() {
        CompteUser foundAccount = agenceService.findAccountByNumber(testCompte.getNumeroCompte().toString());

        assertNotNull(foundAccount);
        assertEquals(testCompte.getNumeroCompte(), foundAccount.getNumeroCompte());
        assertEquals("CLIENT_TEST", foundAccount.getIdClient());
    }

    @Test
    void testFindAccountByNumber_NotFound() {
        assertThrows(Exception.class, () -> {
            agenceService.findAccountByNumber("999999999");
        });
    }
}