package com.serviceAgence;

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

import com.serviceAgence.dto.TransactionRequest;
import com.serviceAgence.dto.TransactionResult;
import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.enums.TransactionStatus;
import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.exception.TransactionException;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.model.Transaction;
import com.serviceAgence.repository.CompteRepository;
import com.serviceAgence.repository.TransactionRepository;
import com.serviceAgence.services.FraisService;
import com.serviceAgence.services.NotificationService;
import com.serviceAgence.services.TransactionService;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

        @Mock
        private CompteRepository compteRepository;
        @Mock
        private TransactionRepository transactionRepository;
        @Mock
        private FraisService fraisService;
        @Mock
        private NotificationService notificationService;
        @InjectMocks
        private TransactionService transactionService;

        private TransactionRequest validTransferRequest;
        private TransactionRequest validDepotRequest;
        private TransactionRequest validRetraitRequest;
        private CompteUser sourceAccount;
        private CompteUser destinationAccount;
        private Transaction mockTransaction;

        @BeforeEach
        void setUp() {
                // Request de transfert
                validTransferRequest = new TransactionRequest();
                validTransferRequest.setType(TransactionType.TRANSFERT_INTERNE);
                validTransferRequest.setMontant(new BigDecimal("1000"));
                validTransferRequest.setCompteSource("123456789");
                validTransferRequest.setCompteDestination("987654321");
                validTransferRequest.setIdClient("CLIENT123");
                validTransferRequest.setIdAgence("AGENCE001");
                validTransferRequest.setDescription("Test transfert");

                // Request de dépôt
                validDepotRequest = new TransactionRequest();
                validDepotRequest.setType(TransactionType.DEPOT_PHYSIQUE);
                validDepotRequest.setMontant(new BigDecimal("2000"));
                validDepotRequest.setCompteSource("123456789");
                validDepotRequest.setIdClient("CLIENT123");
                validDepotRequest.setIdAgence("AGENCE001");

                // Request de retrait (NOUVEAU)
                validRetraitRequest = new TransactionRequest();
                validRetraitRequest.setType(TransactionType.RETRAIT_PHYSIQUE);
                validRetraitRequest.setMontant(new BigDecimal("500"));
                validRetraitRequest.setCompteSource("123456789");
                validRetraitRequest.setIdClient("CLIENT123");
                validRetraitRequest.setIdAgence("AGENCE001");

                // Compte source avec solde suffisant
                sourceAccount = new CompteUser();
                sourceAccount.setNumeroCompte(123456789L);
                sourceAccount.setSolde(new BigDecimal("5000"));
                sourceAccount.setStatus(CompteStatus.ACTIVE);
                sourceAccount.setIdClient("CLIENT123");
                sourceAccount.setIdAgence("AGENCE001");
                sourceAccount.setLimiteDailyWithdrawal(new BigDecimal("1000000"));
                sourceAccount.setLimiteDailyTransfer(new BigDecimal("2000000"));
                sourceAccount.setTotalDailyWithdrawals(BigDecimal.ZERO);
                sourceAccount.setTotalDailyTransfers(BigDecimal.ZERO);

                // Compte destination
                destinationAccount = new CompteUser();
                destinationAccount.setNumeroCompte(987654321L);
                destinationAccount.setSolde(new BigDecimal("2000"));
                destinationAccount.setStatus(CompteStatus.ACTIVE);
                destinationAccount.setIdClient("CLIENT456");
                destinationAccount.setIdAgence("AGENCE001");

                // Transaction mock
                mockTransaction = new Transaction();
                mockTransaction.setId("TXN_123");
                mockTransaction.setTransactionId("TXN_20231201_001");
                mockTransaction.setType(TransactionType.TRANSFERT_INTERNE);
                mockTransaction.setMontant(new BigDecimal("1000"));
                mockTransaction.setFrais(new BigDecimal("50"));
                mockTransaction.setStatus(TransactionStatus.COMPLETED);
                mockTransaction.setCreatedAt(LocalDateTime.now());
        }

        @Test
        void testProcessTransaction_TransferSuccess2() {
                // Given
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));
                when(compteRepository.findByNumeroCompte(987654321L))
                                .thenReturn(Optional.of(destinationAccount));
                when(fraisService.calculateFrais(TransactionType.TRANSFERT_INTERNE,
                                new BigDecimal("1000"), "AGENCE001"))
                                .thenReturn(new BigDecimal("50"));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenReturn(mockTransaction);

                // When
                TransactionResult result = transactionService.processTransaction(validTransferRequest);

                // Then
                assertTrue(result.isSuccess());
                assertNotNull(result.getTransactionId());
                assertEquals(new BigDecimal("1000"), result.getMontant());
                assertEquals(new BigDecimal("50"), result.getFrais());

                verify(compteRepository, times(2)).save(any(CompteUser.class));
                verify(transactionRepository, times(2)).save(any(Transaction.class));
                verify(notificationService).sendTransactionNotification(any(Transaction.class));
        }

        @Test
        void testProcessTransaction_DepotSuccess3() {
                // Given
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));
                when(fraisService.calculateFrais(TransactionType.DEPOT_PHYSIQUE,
                                new BigDecimal("2000"), "AGENCE001"))
                                .thenReturn(BigDecimal.ZERO); // Dépôt gratuit

                Transaction depotTransaction = new Transaction();
                depotTransaction.setTransactionId("TXN_DEPOT_001");
                depotTransaction.setMontant(new BigDecimal("2000"));
                depotTransaction.setFrais(BigDecimal.ZERO);

                when(transactionRepository.save(any(Transaction.class)))
                                .thenReturn(depotTransaction);

                // When
                TransactionResult result = transactionService.processTransaction(validDepotRequest);

                // Then
                assertTrue(result.isSuccess());
                assertEquals(BigDecimal.ZERO, result.getFrais());

                verify(compteRepository).save(sourceAccount);
                verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        void testCompteUser_isActive() {
                CompteUser compte = new CompteUser();
                compte.setStatus(CompteStatus.ACTIVE);
                compte.setBlocked(false);

                assertTrue(compte.isActive(), "Le compte devrait être actif");

                // Test avec compte bloqué
                compte.setBlocked(true);
                assertFalse(compte.isActive(), "Le compte bloqué ne devrait pas être actif");

                // Test avec statut suspendu
                compte.setBlocked(false);
                compte.setStatus(CompteStatus.SUSPENDED);
                assertFalse(compte.isActive(), "Le compte suspendu ne devrait pas être actif");
        }

        // ===================================
        // TEST AVEC UN TRANSFERT (plus simple à déboguer)
        // ===================================

        @Test
        void testGetAccountBalance_CompteNotFound2() {
                // Given
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.empty());

                // When & Then
                TransactionException exception = assertThrows(TransactionException.class, () -> {
                        transactionService.getAccountBalance("123456789");
                });

                assertEquals("COMPTE_INTROUVABLE", exception.getErrorCode());
        }

        @Test
        void testDepot_DoitReussir() {
                // Un dépôt ne doit PAS vérifier le solde, donc doit réussir même avec solde
                // faible
                CompteUser compte = new CompteUser();
                compte.setNumeroCompte(123456789L);
                compte.setSolde(new BigDecimal("1")); // Très peu
                compte.setStatus(CompteStatus.ACTIVE);
                compte.setIdClient("CLIENT123");
                compte.setIdAgence("AGENCE001");
                compte.setBlocked(false);

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(compte));
                when(fraisService.calculateFrais(TransactionType.DEPOT_PHYSIQUE,
                                new BigDecimal("1000"), "AGENCE001"))
                                .thenReturn(BigDecimal.ZERO);
                when(transactionRepository.save(any(Transaction.class)))
                                .thenReturn(new Transaction());

                TransactionRequest request = new TransactionRequest();
                request.setType(TransactionType.DEPOT_PHYSIQUE); // DÉPÔT
                request.setMontant(new BigDecimal("1000"));
                request.setCompteSource("123456789");
                request.setIdClient("CLIENT123");
                request.setIdAgence("AGENCE001");

                // Un dépôt doit réussir
                TransactionResult result = transactionService.processTransaction(request);
                assertTrue(result.isSuccess(), "Un dépôt doit toujours réussir");
        }

        @Test
        void testProcessTransaction_TransferSuccess() {
                // Given
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));
                when(compteRepository.findByNumeroCompte(987654321L))
                                .thenReturn(Optional.of(destinationAccount));
                when(fraisService.calculateFrais(TransactionType.TRANSFERT_INTERNE,
                                new BigDecimal("1000"), "AGENCE001"))
                                .thenReturn(new BigDecimal("50"));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenReturn(mockTransaction);

                // When
                TransactionResult result = transactionService.processTransaction(validTransferRequest);

                // Then
                assertTrue(result.isSuccess());
                assertNotNull(result.getTransactionId());
                assertEquals(new BigDecimal("1000"), result.getMontant());
                assertEquals(new BigDecimal("50"), result.getFrais());

                verify(compteRepository, times(2)).save(any(CompteUser.class));
                verify(transactionRepository, times(2)).save(any(Transaction.class));
                verify(notificationService).sendTransactionNotification(any(Transaction.class));
        }

        @Test
        void testProcessTransaction_DepotSuccess() {
                // Gardez votre test existant tel quel
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));
                when(fraisService.calculateFrais(TransactionType.DEPOT_PHYSIQUE,
                                new BigDecimal("2000"), "AGENCE001"))
                                .thenReturn(BigDecimal.ZERO);

                Transaction depotTransaction = new Transaction();
                depotTransaction.setTransactionId("TXN_DEPOT_001");
                depotTransaction.setMontant(new BigDecimal("2000"));
                depotTransaction.setFrais(BigDecimal.ZERO);

                when(transactionRepository.save(any(Transaction.class)))
                                .thenReturn(depotTransaction);

                TransactionResult result = transactionService.processTransaction(validDepotRequest);

                assertTrue(result.isSuccess());
                assertEquals(BigDecimal.ZERO, result.getFrais());
                verify(compteRepository).save(sourceAccount);
                verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        // ===================================
        // TESTS D'ÉCHEC CORRIGÉS
        // ===================================

        @Test
        void testProcessTransaction_InsufficientBalance() {
                // Given - Compte avec solde insuffisant
                CompteUser compteInsuffisant = new CompteUser();
                compteInsuffisant.setNumeroCompte(123456789L);
                compteInsuffisant.setSolde(new BigDecimal("100"));
                compteInsuffisant.setStatus(CompteStatus.ACTIVE);
                compteInsuffisant.setIdClient("CLIENT123");
                compteInsuffisant.setIdAgence("AGENCE001");
                compteInsuffisant.setBlocked(false);
                compteInsuffisant.setLimiteDailyWithdrawal(new BigDecimal("1000000"));
                compteInsuffisant.setTotalDailyWithdrawals(BigDecimal.ZERO);

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(compteInsuffisant));
                when(fraisService.calculateFrais(TransactionType.RETRAIT_PHYSIQUE,
                                new BigDecimal("500"), "AGENCE001"))
                                .thenReturn(new BigDecimal("50"));

                // When
                TransactionResult result = transactionService.processTransaction(validRetraitRequest);

                // Then - Vérifier l'échec
                assertFalse(result.isSuccess(), "La transaction devrait échouer");
                assertEquals("SOLDE_INSUFFISANT", result.getErrorCode());
                assertTrue(result.getMessage().contains("Solde insuffisant"));

                // Vérifier qu'aucune sauvegarde n'a eu lieu
                verify(compteRepository, never()).save(any(CompteUser.class));
                verify(transactionRepository, never()).save(any(Transaction.class));
                verify(notificationService, never()).sendTransactionNotification(any(Transaction.class));
        }

        @Test
        void testProcessTransaction_InsufficientBalance_Transfer() {
                // Given
                CompteUser compteInsuffisant = new CompteUser();
                compteInsuffisant.setNumeroCompte(123456789L);
                compteInsuffisant.setSolde(new BigDecimal("50"));
                compteInsuffisant.setStatus(CompteStatus.ACTIVE);
                compteInsuffisant.setIdClient("CLIENT123");
                compteInsuffisant.setIdAgence("AGENCE001");
                compteInsuffisant.setBlocked(false);
                compteInsuffisant.setLimiteDailyTransfer(new BigDecimal("2000000"));
                compteInsuffisant.setTotalDailyTransfers(BigDecimal.ZERO);

                CompteUser compteDestination = new CompteUser();
                compteDestination.setNumeroCompte(987654321L);
                compteDestination.setSolde(new BigDecimal("1000"));
                compteDestination.setStatus(CompteStatus.ACTIVE);
                compteDestination.setBlocked(false);

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(compteInsuffisant));
                when(compteRepository.findByNumeroCompte(987654321L))
                                .thenReturn(Optional.of(compteDestination));
                when(fraisService.calculateFrais(TransactionType.TRANSFERT_INTERNE,
                                new BigDecimal("1000"), "AGENCE001"))
                                .thenReturn(new BigDecimal("50"));

                // When
                TransactionResult result = transactionService.processTransaction(validTransferRequest);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("SOLDE_INSUFFISANT", result.getErrorCode());
                assertTrue(result.getMessage().contains("Solde insuffisant"));
        }

        @Test
        void testProcessTransaction_CompteInactif() {
                // Given - Compte inactif
                CompteUser compteInactif = new CompteUser();
                compteInactif.setNumeroCompte(123456789L);
                compteInactif.setSolde(new BigDecimal("5000")); // Solde suffisant
                compteInactif.setStatus(CompteStatus.SUSPENDED); // Mais compte suspendu
                compteInactif.setBlocked(false);

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(compteInactif));

                // When
                TransactionResult result = transactionService.processTransaction(validRetraitRequest);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("COMPTE_INACTIF", result.getErrorCode());
                assertTrue(result.getMessage().contains("compte source n'est pas actif"));
        }

        @Test
        void testProcessTransaction_CompteBloque() {
                // Given - Compte bloqué
                CompteUser compteBloque = new CompteUser();
                compteBloque.setNumeroCompte(123456789L);
                compteBloque.setSolde(new BigDecimal("5000"));
                compteBloque.setStatus(CompteStatus.ACTIVE);
                compteBloque.setBlocked(true); // Compte bloqué

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(compteBloque));

                // When
                TransactionResult result = transactionService.processTransaction(validRetraitRequest);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("COMPTE_INACTIF", result.getErrorCode());
        }

        @Test
        void testProcessTransaction_CompteIntrouvable() {
                // Given - Compte n'existe pas
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.empty());

                // When
                TransactionResult result = transactionService.processTransaction(validRetraitRequest);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("COMPTE_INTROUVABLE", result.getErrorCode());
                assertTrue(result.getMessage().contains("introuvable"));
        }

        @Test
        void testProcessTransaction_MontantInvalide() {
                // Given - Montant négatif
                TransactionRequest requestInvalide = new TransactionRequest();
                requestInvalide.setType(TransactionType.RETRAIT_PHYSIQUE);
                requestInvalide.setMontant(new BigDecimal("-100")); // Négatif
                requestInvalide.setCompteSource("123456789");
                requestInvalide.setIdClient("CLIENT123");
                requestInvalide.setIdAgence("AGENCE001");

                // When
                TransactionResult result = transactionService.processTransaction(requestInvalide);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("MONTANT_INVALIDE", result.getErrorCode());
                assertTrue(result.getMessage().contains("positif"));
        }

        @Test
        void testProcessTransaction_MontantTropEleve() {
                // Given - Montant trop élevé
                TransactionRequest requestTropEleve = new TransactionRequest();
                requestTropEleve.setType(TransactionType.RETRAIT_PHYSIQUE);
                requestTropEleve.setMontant(new BigDecimal("60000000")); // > 50M
                requestTropEleve.setCompteSource("123456789");
                requestTropEleve.setIdClient("CLIENT123");
                requestTropEleve.setIdAgence("AGENCE001");

                // When
                TransactionResult result = transactionService.processTransaction(requestTropEleve);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("MONTANT_TROP_ELEVE", result.getErrorCode());
                assertTrue(result.getMessage().contains("maximum"));
        }

        @Test
        void testProcessTransaction_CompteDestinationRequis() {
                // Given - Transfert sans destination
                TransactionRequest transferSansDestination = new TransactionRequest();
                transferSansDestination.setType(TransactionType.TRANSFERT_INTERNE);
                transferSansDestination.setMontant(new BigDecimal("1000"));
                transferSansDestination.setCompteSource("123456789");
                transferSansDestination.setCompteDestination(null); // Pas de destination
                transferSansDestination.setIdClient("CLIENT123");
                transferSansDestination.setIdAgence("AGENCE001");

                // When
                TransactionResult result = transactionService.processTransaction(transferSansDestination);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("COMPTE_DESTINATION_REQUIS", result.getErrorCode());
        }

        @Test
        void testProcessTransaction_CompteDestinationInactif() {
                // Given
                CompteUser compteDestinationInactif = new CompteUser();
                compteDestinationInactif.setNumeroCompte(987654321L);
                compteDestinationInactif.setSolde(new BigDecimal("1000"));
                compteDestinationInactif.setStatus(CompteStatus.SUSPENDED); // Inactif
                compteDestinationInactif.setBlocked(false);

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));
                when(compteRepository.findByNumeroCompte(987654321L))
                                .thenReturn(Optional.of(compteDestinationInactif));

                // When
                TransactionResult result = transactionService.processTransaction(validTransferRequest);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("ERREUR_TECHNIQUE", result.getErrorCode());
        }

        @Test
        void testProcessTransaction_AutoTransfert() {
                // Given - Transfert vers le même compte
                TransactionRequest autoTransfert = new TransactionRequest();
                autoTransfert.setType(TransactionType.TRANSFERT_INTERNE);
                autoTransfert.setMontant(new BigDecimal("1000"));
                autoTransfert.setCompteSource("123456789");
                autoTransfert.setCompteDestination("123456789"); // Même compte
                autoTransfert.setIdClient("CLIENT123");
                autoTransfert.setIdAgence("AGENCE001");

                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));

                // When
                TransactionResult result = transactionService.processTransaction(autoTransfert);

                // Then
                assertFalse(result.isSuccess());
                assertEquals("ERREUR_TECHNIQUE", result.getErrorCode());
        }

        // ===================================
        // TESTS UTILITAIRES (à modifier aussi)
        // ===================================

        @Test
        void testGetAccountBalance_CompteNotFound() {
                // Given
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.empty());

                // When & Then - ICI C'EST DIFFÉRENT : getAccountBalance LANCE une exception
                TransactionException exception = assertThrows(TransactionException.class, () -> {
                        transactionService.getAccountBalance("123456789");
                });

                assertEquals("COMPTE_INTROUVABLE", exception.getErrorCode());
        }

        // ===================================
        // TESTS DE SUCCÈS (gardez-les)
        // ===================================

        @Test
        void testGetAccountBalance() {
                // Gardez tel quel
                when(compteRepository.findByNumeroCompte(123456789L))
                                .thenReturn(Optional.of(sourceAccount));

                BigDecimal balance = transactionService.getAccountBalance("123456789");

                assertEquals(new BigDecimal("5000"), balance);
        }
}