package com.m1_fonda.serviceUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.m1_fonda.serviceUser.event.DepotEvent;
import com.m1_fonda.serviceUser.event.PasswordResetEvent;
import com.m1_fonda.serviceUser.event.RetraitEvent;
import com.m1_fonda.serviceUser.event.TransactionEvent;
import com.m1_fonda.serviceUser.event.UserRegistrationEvent;
import com.m1_fonda.serviceUser.event.WelcomeNotificationEvent;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.pojo.PasswordResetConfirmation;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.DepositRequest;
import com.m1_fonda.serviceUser.request.PasswordResetRequest;
import com.m1_fonda.serviceUser.request.TransferRequest;
import com.m1_fonda.serviceUser.request.WithdrawalRequest;
import com.m1_fonda.serviceUser.response.RegistrationResponse;
import com.m1_fonda.serviceUser.response.TransactionResponse;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceRabbit Tests")
class UserServiceRabbitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceRabbit userServiceRabbit;

    private Client testClient;
    private final long RESPONSE_TIMEOUT = 30000L;

    @BeforeEach
    void setUp() {
        // Configuration du timeout via reflection
        ReflectionTestUtils.setField(userServiceRabbit, "responseTimeout", RESPONSE_TIMEOUT);

        // Client de test
        testClient = new Client();
        testClient.setIdClient("client-123");
        testClient.setEmail("test@example.com");
        testClient.setCni("123456789");
        testClient.setNumero("654123456");
        testClient.setNom("DUPONT");
        testClient.setPrenom("Jean");
        testClient.setStatus(ClientStatus.ACTIVE);
        testClient.setIdAgence("AGENCE001");
        testClient.setRectoCni("recto.jpg");
        testClient.setVersoCni("verso.jpg");
    }

    @Nested
    @DisplayName("Tests d'envoi d'événements d'enregistrement")
    class RegistrationEventTests {

        @Test
        @DisplayName("Envoi événement d'enregistrement - Succès")
        void sendRegistrationEvent_WithValidClient_ShouldSendEvent() {
            // When
            userServiceRabbit.sendRegistrationEvent(testClient);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    eq("Client-exchange"),
                    eq("demande.send"),
                    any(UserRegistrationEvent.class));

            // Vérifier que le mot de passe n'est PAS envoyé
            // (UserRegistrationEvent ne contient pas de champ password)
        }

        @Test
        @DisplayName("Envoi événement avec erreur RabbitMQ - ServiceException")
        void sendRegistrationEvent_WithRabbitMQError_ShouldThrowServiceException() {
            // Given
            doThrow(new AmqpException("Connection failed"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            // When & Then
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> userServiceRabbit.sendRegistrationEvent(testClient));

            assertEquals("Service AgenceService temporairement indisponible", exception.getMessage());
        }

        @Test
        @DisplayName("Envoi événement avec erreur générique - ServiceException")
        void sendRegistrationEvent_WithGenericError_ShouldThrowServiceException() {
            // Given
            doThrow(new RuntimeException("Unexpected error"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            // When & Then
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> userServiceRabbit.sendRegistrationEvent(testClient));

            assertEquals("Erreur technique lors de l'envoi de la demande", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Tests de gestion des réponses d'enregistrement")
    class RegistrationResponseTests {

        @Test
        @DisplayName("Traitement réponse ACCEPTE - Active le compte")
        void handleRegistrationResponse_WithAcceptedStatus_ShouldActivateAccount() {
            // Given
            RegistrationResponse response = new RegistrationResponse();
            response.setStatut("ACCEPTE");
            response.setEmail("test@example.com");
            response.setNumeroCompte(123456789L);
            response.setIdClient("client-123");

            when(repository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testClient));
            when(repository.save(any(Client.class)))
                    .thenReturn(testClient);

            // When
            userServiceRabbit.handleRegistrationResponse(response);

            // Then
            assertEquals(ClientStatus.ACTIVE, testClient.getStatus());
            verify(repository).save(testClient);

            // Vérifier envoi notification bienvenue
            verify(rabbitTemplate).convertAndSend(
                    eq("Notification-exchange"),
                    eq("welcome.send"),
                    any(WelcomeNotificationEvent.class));
        }

        @Test
        @DisplayName("Traitement réponse REFUSE - Rejette le compte")
        void handleRegistrationResponse_WithRejectedStatus_ShouldRejectAccount() {
            // Given
            RegistrationResponse response = new RegistrationResponse();
            response.setStatut("REFUSE");
            response.setEmail("test@example.com");
            response.setProbleme("Documents non conformes");

            when(repository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testClient));
            when(repository.save(any(Client.class)))
                    .thenReturn(testClient);

            // When
            userServiceRabbit.handleRegistrationResponse(response);

            // Then
            assertEquals(ClientStatus.REJECTED, testClient.getStatus());
            verify(repository).save(testClient);

            // Vérifier envoi notification rejet
            verify(rabbitTemplate).convertAndSend(
                    eq("Notification-exchange"),
                    eq("rejection.send"),
                    any(Object.class));
        }

        @Test
        @DisplayName("Traitement réponse avec statut inconnu - Log warning")
        void handleRegistrationResponse_WithUnknownStatus_ShouldLogWarning() {
            // Given
            RegistrationResponse response = new RegistrationResponse();
            response.setStatut("UNKNOWN_STATUS");

            // When
            userServiceRabbit.handleRegistrationResponse(response);

            // Then
            // Aucune action ne doit être prise
            verify(repository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("Tests d'opérations financières")
    class FinancialOperationsTests {

        @Test
        @DisplayName("Envoi dépôt - Succès")
        void sendDepot_WithValidRequest_ShouldReturnSuccess() {
            // Given
            DepositRequest request = new DepositRequest();
            request.setMontant(new BigDecimal("1000"));
            request.setNumeroClient("client-123");
            request.setNumeroCompte(123456789L);

            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            TransactionResponse mockResponse = new TransactionResponse(
                    "txn-123", "SUCCESS", "Dépôt réussi",
                    new BigDecimal("1000"), LocalDateTime.now());

            when(rabbitTemplate.convertSendAndReceive(
                    eq("Client-exchange"), eq("depot.send"), any(DepotEvent.class)))
                    .thenReturn(mockResponse);

            // When
            TransactionResponse response = userServiceRabbit.sendDepot(request, "client-123");

            // Then
            assertNotNull(response);
            assertEquals("txn-123", response.getTransactionId());
            assertEquals("SUCCESS", response.getStatus());

            verify(rabbitTemplate).setReceiveTimeout(RESPONSE_TIMEOUT);

            verify(rabbitTemplate).convertSendAndReceive(
                    eq("Client-exchange"), eq("depot.send"), any(DepotEvent.class));
        }

        @Test
        @DisplayName("Envoi dépôt avec client inactif - BusinessValidationException")
        void sendDepot_WithInactiveClient_ShouldThrowException() {
            // Given
            DepositRequest request = new DepositRequest();
            request.setMontant(new BigDecimal("1000"));
            request.setNumeroClient("client-123");
            request.setNumeroCompte(123456789L);

            testClient.setStatus(ClientStatus.BLOCKED);
            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendDepot(request, "client-123"));

            assertEquals("Compte non actif", exception.getMessage());
        }

        @Test
        @DisplayName("Envoi dépôt avec montant trop élevé - BusinessValidationException")
        void sendDepot_WithExcessiveAmount_ShouldThrowException() {
            // Given
            DepositRequest request = new DepositRequest();
            request.setMontant(new BigDecimal("15000000")); // > 10M limite
            request.setNumeroClient("client-123");
            request.setNumeroCompte(123456789L);

            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendDepot(request, "client-123"));

            assertEquals("Montant trop élevé", exception.getMessage());
        }

        @Test
        @DisplayName("Envoi retrait - Succès")
        void sendRetrait_WithValidRequest_ShouldReturnSuccess() {
            // Given
            WithdrawalRequest request = new WithdrawalRequest();
            request.setMontant(new BigDecimal("500"));
            request.setNumeroClient("client-123");
            request.setNumeroCompte(123456789L);

            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            TransactionResponse mockResponse = new TransactionResponse(
                    "txn-456", "SUCCESS", "Retrait réussi",
                    new BigDecimal("500"), LocalDateTime.now());

            when(rabbitTemplate.convertSendAndReceive(
                    eq("Client-exchange"), eq("retrait.send"), any(RetraitEvent.class)))
                    .thenReturn(mockResponse);

            // When
            TransactionResponse response = userServiceRabbit.sendRetrait(request, "client-123");

            // Then
            assertNotNull(response);
            assertEquals("txn-456", response.getTransactionId());
            assertEquals("SUCCESS", response.getStatus());
        }

        @Test
        @DisplayName("Envoi transaction inter-comptes - Succès")
        void sendTransaction_WithValidRequest_ShouldReturnSuccess() {
            // Given
            TransferRequest request = new TransferRequest();
            request.setMontant(new BigDecimal("750"));
            request.setNumeroCompteSend(123456789L);
            request.setNumeroCompteReceive(987654321L);

            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            TransactionResponse mockResponse = new TransactionResponse(
                    "txn-789", "SUCCESS", "Transfert réussi",
                    new BigDecimal("750"), LocalDateTime.now());

            when(rabbitTemplate.convertSendAndReceive(
                    eq("Client-exchange"), eq("transaction.send"), any(TransactionEvent.class)))
                    .thenReturn(mockResponse);

            // When
            TransactionResponse response = userServiceRabbit.sendTransaction(request, "client-123");

            // Then
            assertNotNull(response);
            assertEquals("txn-789", response.getTransactionId());

            ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
            verify(rabbitTemplate).convertSendAndReceive(
                    eq("Client-exchange"), eq("transaction.send"), eventCaptor.capture());

            TransactionEvent sentEvent = eventCaptor.getValue();
            assertEquals(new BigDecimal("750"), sentEvent.getMontant());
            assertEquals(Long.valueOf(123456789L), sentEvent.getNumeroCompteSend());
            assertEquals(Long.valueOf(987654321L), sentEvent.getNumeroCompteReceive());
            assertEquals("client-123", sentEvent.getClientId());
        }

        @Test
        @DisplayName("Transfert vers même compte - BusinessValidationException")
        void sendTransaction_WithSameAccount_ShouldThrowException() {
            // Given
            TransferRequest request = new TransferRequest();
            request.setMontant(new BigDecimal("500"));
            request.setNumeroCompteSend(123456789L);
            request.setNumeroCompteReceive(123456789L); // Même compte

            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendTransaction(request, "client-123"));

            assertEquals("Impossible de transférer vers le même compte", exception.getMessage());
        }

        @Test
        @DisplayName("Opération avec timeout - ServiceException")
        void sendDepot_WithTimeout_ShouldThrowServiceException() {
            // Given
            DepositRequest request = new DepositRequest();
            request.setMontant(new BigDecimal("1000"));
            request.setNumeroClient("client-123");
            request.setNumeroCompte(123456789L);

            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            when(rabbitTemplate.convertSendAndReceive(anyString(), anyString(), any(Object.class)))
                    .thenThrow(new AmqpTimeoutException("Timeout"));

            // When & Then
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> userServiceRabbit.sendDepot(request, "client-123"));

            assertEquals("Service de dépôt temporairement indisponible", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Tests de réinitialisation de mot de passe")
    class PasswordResetTests {

        @Test
        @DisplayName("Envoi demande reset password - Succès")
        void sendPasswordResetRequest_WithValidData_ShouldSendEvent() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setCni("123456789");
            request.setEmail("test@example.com");
            request.setNumero("654123456");
            request.setNom("DUPONT");

            when(repository.findByCni("123456789"))
                    .thenReturn(Optional.of(testClient));

            // When
            userServiceRabbit.sendPasswordResetRequest(request);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    eq("Client-exchange"),
                    eq("connexion.send"),
                    any(PasswordResetEvent.class));
        }

        @Test
        @DisplayName("Reset password avec client inexistant - BusinessValidationException")
        void sendPasswordResetRequest_WithNonExistentClient_ShouldThrowException() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setCni("999999999");
            request.setEmail("unknown@example.com");

            when(repository.findByCni("999999999"))
                    .thenReturn(Optional.empty());

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendPasswordResetRequest(request));

            assertEquals("Client introuvable", exception.getMessage());
        }

        @Test
        @DisplayName("Reset password avec données non correspondantes - BusinessValidationException")
        void sendPasswordResetRequest_WithMismatchedData_ShouldThrowException() {
            // Given
            PasswordResetRequest request = new PasswordResetRequest();
            request.setCni("123456789");
            request.setEmail("wrong@example.com"); // Email différent
            request.setNumero("654123456");

            when(repository.findByCni("123456789"))
                    .thenReturn(Optional.of(testClient));

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendPasswordResetRequest(request));

            assertEquals("Informations non correspondantes", exception.getMessage());
        }

        @Test
        @DisplayName("Traitement confirmation reset password - Succès")
        void handlePasswordResetConfirmation_WithValidData_ShouldUpdatePassword() {
            // Given
            PasswordResetConfirmation confirmation = new PasswordResetConfirmation();
            confirmation.setCni("123456789");
            confirmation.setNewPassword("NewPassword123!");
            confirmation.setEmail("test@example.com");

            when(repository.findByCni("123456789"))
                    .thenReturn(Optional.of(testClient));
            when(passwordEncoder.encode("NewPassword123!"))
                    .thenReturn("hashedNewPassword");

            // When
            userServiceRabbit.handlePasswordResetConfirmation(confirmation);

            // Then
            verify(repository).updatePassword(
                    eq("client-123"),
                    eq("hashedNewPassword"),
                    anyString(),
                    any(LocalDateTime.class));

            // Vérifier envoi notification changement password
            verify(rabbitTemplate).convertAndSend(
                    eq("Notification-exchange"),
                    eq("password.change"),
                    any(Object.class));
        }
    }

    @Nested
    @DisplayName("Tests de validation métier")
    class BusinessValidationTests {

        @Test
        @DisplayName("Validation client inexistant - BusinessValidationException")
        void validateFinancialOperation_WithNonExistentClient_ShouldThrowException() {
            // Given
            when(repository.findById("invalid-id"))
                    .thenReturn(Optional.empty());

            DepositRequest request = new DepositRequest();
            request.setMontant(new BigDecimal("1000"));

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendDepot(request, "invalid-id"));

            assertEquals("Client introuvable", exception.getMessage());
        }

        @Test
        @DisplayName("Validation montant négatif - BusinessValidationException")
        void validateFinancialOperation_WithNegativeAmount_ShouldThrowException() {
            // Given
            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            DepositRequest request = new DepositRequest();
            request.setMontant(new BigDecimal("-100"));

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendDepot(request, "client-123"));

            assertEquals("Montant invalide", exception.getMessage());
        }

        @Test
        @DisplayName("Validation montant zéro - BusinessValidationException")
        void validateFinancialOperation_WithZeroAmount_ShouldThrowException() {
            // Given
            when(repository.findById("client-123"))
                    .thenReturn(Optional.of(testClient));

            DepositRequest request = new DepositRequest();
            request.setMontant(BigDecimal.ZERO);

            // When & Then
            BusinessValidationException exception = assertThrows(
                    BusinessValidationException.class,
                    () -> userServiceRabbit.sendDepot(request, "client-123"));

            assertEquals("Montant invalide", exception.getMessage());
        }
    }
}