package com.m1_fonda.serviceUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.m1_fonda.serviceUser.event.DepotEvent;
import com.m1_fonda.serviceUser.event.UserRegistrationEvent;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.DepositRequest;
import com.m1_fonda.serviceUser.request.PasswordResetRequest;
import com.m1_fonda.serviceUser.response.TransactionResponse;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

/**
 * Version simplifiée des tests RabbitMQ sans problèmes d'ambiguïté
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceRabbit Simple Tests")
class UserServiceRabbitSimpleTest {

        @Mock
        private RabbitTemplate rabbitTemplate;

        @Mock
        private UserRepository repository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private UserServiceRabbit userServiceRabbit;

        private Client testClient;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(userServiceRabbit, "responseTimeout", 30000L);

                testClient = new Client();
                testClient.setIdClient("client-123");
                testClient.setEmail("test@example.com");
                testClient.setCni("123456789");
                testClient.setNumero("654123456");
                testClient.setNom("DUPONT");
                testClient.setPrenom("Jean");
                testClient.setStatus(ClientStatus.ACTIVE);
                testClient.setIdAgence("AGENCE001");
        }

        @Test
        @DisplayName("Envoi événement avec erreur RabbitMQ - ServiceException")
        void sendRegistrationEvent_WithRabbitMQError_ShouldThrowServiceException() {
                // Given - CORRECTION : Mock de la bonne méthode
                doThrow(new AmqpException("Connection failed"))
                                .when(rabbitTemplate).convertAndSend(
                                                eq("Client-exchange"), // ← Exchange correct
                                                eq("demande.send"), // ← Routing key correct
                                                any(UserRegistrationEvent.class) // ← Event correct
                                );

                // When & Then
                ServiceException exception = assertThrows(
                                ServiceException.class,
                                () -> userServiceRabbit.sendRegistrationEvent(testClient));

                assertEquals("Service AgenceService temporairement indisponible", exception.getMessage());

                // Vérifier que la méthode a été appelée
                verify(rabbitTemplate).convertAndSend(
                                eq("Client-exchange"),
                                eq("demande.send"),
                                any(UserRegistrationEvent.class));
        }

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

                // CORRECTION : Mock avec les 3 paramètres séparés
                when(rabbitTemplate.convertSendAndReceive(
                                eq("Client-exchange"), // ← Paramètre 1 : exchange
                                eq("depot.send"), // ← Paramètre 2 : routing key
                                any(DepotEvent.class) // ← Paramètre 3 : event
                )).thenReturn(mockResponse);

                // When
                TransactionResponse response = userServiceRabbit.sendDepot(request, "client-123");

                // Then
                assertNotNull(response);
                assertEquals("txn-123", response.getTransactionId());
                assertEquals("SUCCESS", response.getStatus());

                // Vérifier que les méthodes ont été appelées
                verify(repository).findById("client-123");
                verify(rabbitTemplate).convertSendAndReceive(
                                eq("Client-exchange"),
                                eq("depot.send"),
                                any(DepotEvent.class));
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
        @DisplayName("Validation client inexistant pour opération financière")
        void sendDepot_WithNonExistentClient_ShouldThrowException() {
                // Given
                DepositRequest request = new DepositRequest();
                request.setMontant(new BigDecimal("1000"));

                when(repository.findById("invalid-id"))
                                .thenReturn(Optional.empty());

                // When & Then
                BusinessValidationException exception = assertThrows(
                                BusinessValidationException.class,
                                () -> userServiceRabbit.sendDepot(request, "invalid-id"));

                assertEquals("Client introuvable", exception.getMessage());
        }

        @Test
        @DisplayName("Validation montant négatif")
        void sendDepot_WithNegativeAmount_ShouldThrowException() {
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
        @DisplayName("Validation montant trop élevé")
        void sendDepot_WithExcessiveAmount_ShouldThrowException() {
                // Given
                when(repository.findById("client-123"))
                                .thenReturn(Optional.of(testClient));

                DepositRequest request = new DepositRequest();
                request.setMontant(new BigDecimal("15000000")); // > 10M limite

                // When & Then
                BusinessValidationException exception = assertThrows(
                                BusinessValidationException.class,
                                () -> userServiceRabbit.sendDepot(request, "client-123"));

                assertEquals("Montant trop élevé", exception.getMessage());
        }
}