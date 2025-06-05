package com.m1_fonda.serviceUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.response.RegisterResponse;
import com.m1_fonda.serviceUser.service.UserService;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

        @InjectMocks
        private UserService userService;

        private ClientRegistrationDTO validRegistrationDTO;
        private Client existingClient;

        @Mock
        private UserRepository repository; // AJOUT : Pour correspondre au champ dans UserService

        @Mock
        private UserServiceRabbit userServiceRabbit;

        @Mock
        private PasswordEncoder passwordEncoder;

        @BeforeEach
        void setUp() {

                validRegistrationDTO = new ClientRegistrationDTO();
                validRegistrationDTO.setCni("123456789");
                validRegistrationDTO.setEmail("test@example.com");
                validRegistrationDTO.setNom("DUPONT");
                validRegistrationDTO.setPrenom("Jean");
                validRegistrationDTO.setNumero("654123456");
                validRegistrationDTO.setPassword("Password123!");
                validRegistrationDTO.setIdAgence("AGENCE001");

                // Client existant pour tests
                existingClient = new Client();
                existingClient.setIdClient("client-123");
                existingClient.setEmail("test@example.com");
                existingClient.setCni("123456789");
                existingClient.setNumero("654123456");
                existingClient.setStatus(ClientStatus.PENDING);
                existingClient.setCreatedAt(LocalDateTime.now());
                // CHANGEMENT : Utilisez le bon nom de champ
                existingClient.setLoginAttempts(0);
        }

        @Nested
        @DisplayName("Tests d'authentification")
        class AuthenticationTests {

                @Test
                @DisplayName("Recherche client par numéro - Succès")
                void findForAuthentication_WithValidPhoneNumber_ShouldReturnClient() {
                        // Given
                        String phoneNumber = "654123456";
                        when(repository.findActiveClientByNumero(phoneNumber))
                                        .thenReturn(Optional.of(existingClient));

                        // When
                        Optional<Client> result = userService.findForAuthentication(phoneNumber);

                        // Then
                        assertTrue(result.isPresent());
                        assertEquals(existingClient.getEmail(), result.get().getEmail());
                        verify(repository).findActiveClientByNumero(phoneNumber);
                        verify(repository, never()).findActiveClientByEmail(anyString());
                }

                @Test
                @DisplayName("Recherche client par email - Succès")
                void findForAuthentication_WithValidEmail_ShouldReturnClient() {
                        // Given
                        String email = "test@example.com";
                        when(repository.findActiveClientByEmail(email))
                                        .thenReturn(Optional.of(existingClient));

                        // When
                        Optional<Client> result = userService.findForAuthentication(email);

                        // Then
                        assertTrue(result.isPresent());
                        assertEquals(existingClient.getEmail(), result.get().getEmail());
                        verify(repository).findActiveClientByEmail(email);
                }

                @Test
                @DisplayName("Recherche avec identifiant invalide - Retourne vide")
                void findForAuthentication_WithInvalidIdentifier_ShouldReturnEmpty() {
                        // Given
                        String invalidIdentifier = "invalid123";

                        // When
                        Optional<Client> result = userService.findForAuthentication(invalidIdentifier);

                        // Then
                        assertTrue(result.isEmpty());
                        verify(repository, never()).findActiveClientByNumero(anyString());
                        verify(repository, never()).findActiveClientByEmail(anyString());
                }
        }

        @Nested
        @DisplayName("Tests d'enregistrement")
        class RegistrationTests {

                @Test
                @DisplayName("Enregistrement valide - Succès")
                void register_WithValidData_ShouldReturnSuccessResponse() {
                        // Given - CHANGEMENT : userRepository au lieu de repository
                        when(repository.findByEmail(validRegistrationDTO.getEmail()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByCni(validRegistrationDTO.getCni()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByNumero(validRegistrationDTO.getNumero()))
                                        .thenReturn(Optional.empty());
                        when(passwordEncoder.encode(anyString()))
                                        .thenReturn("hashedPassword");

                        // When
                        RegisterResponse response = userService.register(validRegistrationDTO);

                        // Then
                        assertNotNull(response);
                        assertEquals("PENDING", response.getStatus());
                        assertTrue(response.getMessage().contains("transmise"));
                        verify(userServiceRabbit).sendRegistrationEvent(any(Client.class));
                }

                @Test
                @DisplayName("Enregistrement avec email existant - Erreur")
                void register_WithExistingEmail_ShouldThrowBusinessValidationException() {
                        // Given
                        when(repository.findByEmail(validRegistrationDTO.getEmail()))
                                        .thenReturn(Optional.of(existingClient));

                        // When & Then
                        BusinessValidationException exception = assertThrows(
                                        BusinessValidationException.class,
                                        () -> userService.register(validRegistrationDTO));

                        assertEquals("Un compte existe déjà avec cet email", exception.getMessage());
                        verify(userServiceRabbit, never()).sendRegistrationEvent(any());
                }

                @Test
                @DisplayName("Enregistrement avec CNI existante - Erreur")
                void register_WithExistingCNI_ShouldThrowBusinessValidationException() {
                        // Given
                        when(repository.findByEmail(validRegistrationDTO.getEmail()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByCni(validRegistrationDTO.getCni()))
                                        .thenReturn(Optional.of(existingClient));

                        // When & Then
                        BusinessValidationException exception = assertThrows(
                                        BusinessValidationException.class,
                                        () -> userService.register(validRegistrationDTO));

                        assertEquals("Un compte existe déjà avec cette CNI", exception.getMessage());
                }

                @Test
                @DisplayName("Enregistrement avec numéro existant - Erreur")
                void register_WithExistingNumber_ShouldThrowBusinessValidationException() {
                        // Given
                        when(repository.findByEmail(validRegistrationDTO.getEmail()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByCni(validRegistrationDTO.getCni()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByNumero(validRegistrationDTO.getNumero()))
                                        .thenReturn(Optional.of(existingClient));

                        // When & Then
                        BusinessValidationException exception = assertThrows(
                                        BusinessValidationException.class,
                                        () -> userService.register(validRegistrationDTO));

                        assertEquals("Un compte existe déjà avec ce numéro", exception.getMessage());
                }

                @Test
                @DisplayName("Enregistrement avec erreur RabbitMQ - ServiceException")
                void register_WithRabbitMQError_ShouldThrowServiceException() {
                        // Given
                        when(repository.findByEmail(validRegistrationDTO.getEmail()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByCni(validRegistrationDTO.getCni()))
                                        .thenReturn(Optional.empty());
                        when(repository.findByNumero(validRegistrationDTO.getNumero()))
                                        .thenReturn(Optional.empty());
                        when(passwordEncoder.encode(anyString()))
                                        .thenReturn("hashedPassword");

                        doThrow(new RuntimeException("RabbitMQ connection failed"))
                                        .when(userServiceRabbit).sendRegistrationEvent(any(Client.class));

                        // When & Then
                        ServiceException exception = assertThrows(
                                        ServiceException.class,
                                        () -> userService.register(validRegistrationDTO));

                        assertEquals("Erreur technique lors de l'enregistrement", exception.getMessage());
                }
        }

        @Nested
        @DisplayName("Tests de gestion des tentatives de connexion")
        class LoginAttemptsTests {

                @Test
                @DisplayName("Connexion réussie - Met à jour lastLogin")
                void recordSuccessfulLogin_ShouldUpdateLastLogin() {
                        // Given
                        String clientId = "client-123";

                        // When
                        userService.recordSuccessfulLogin(clientId);

                        // Then
                        verify(repository).updateLastLogin(eq(clientId), any(LocalDateTime.class));
                }

                @Test
                @DisplayName("Connexion échouée - Incrémente tentatives")
                void recordFailedLogin_ShouldIncrementAttempts() {
                        // Given
                        String clientId = "client-123";
                        existingClient.setLoginAttempts(2); // Moins de 5
                        when(repository.findById(clientId))
                                        .thenReturn(Optional.of(existingClient));

                        // When
                        userService.recordFailedLogin(clientId);

                        // Then
                        verify(repository).incrementFailedLoginAttempts(eq(clientId), any(LocalDateTime.class));
                        verify(repository, never()).updateStatus(anyString(), any(), any());
                }

                @Test
                @DisplayName("Trop de tentatives échouées - Bloque le compte")
                void recordFailedLogin_WithTooManyAttempts_ShouldBlockAccount() {
                        // Given
                        String clientId = "client-123";
                        existingClient.setLoginAttempts(5); // 5 tentatives = blocage
                        when(repository.findById(clientId))
                                        .thenReturn(Optional.of(existingClient));

                        // When
                        userService.recordFailedLogin(clientId);

                        // Then
                        verify(repository).incrementFailedLoginAttempts(eq(clientId), any(LocalDateTime.class));
                        verify(repository).updateStatus(eq(clientId), eq(ClientStatus.BLOCKED),
                                        any(LocalDateTime.class));
                }
        }

        @Nested
        @DisplayName("Tests de validation d'unicité")
        class UniquenessValidationTests {

                @Test
                @DisplayName("Validation unicité - Toutes données uniques")
                void validateUniqueness_WithUniqueData_ShouldNotThrowException() {
                        // Given
                        when(repository.existsByEmail("unique@example.com")).thenReturn(false);
                        when(repository.existsByCni("987654321")).thenReturn(false);
                        when(repository.existsByNumero("659876543")).thenReturn(false);

                        // When & Then
                        assertDoesNotThrow(() -> userService.validateUniqueness("unique@example.com", "987654321",
                                        "659876543"));
                }

                @Test
                @DisplayName("Validation unicité - Email déjà existant")
                void validateUniqueness_WithExistingEmail_ShouldThrowException() {
                        // Given
                        when(repository.existsByEmail("existing@example.com")).thenReturn(true);

                        // When & Then
                        BusinessValidationException exception = assertThrows(
                                        BusinessValidationException.class,
                                        () -> userService.validateUniqueness("existing@example.com", "987654321",
                                                        "659876543"));

                        assertEquals("Un compte existe déjà avec cet email", exception.getMessage());
                }
        }

        @Nested
        @DisplayName("Tests de gestion des statuts de compte")
        class AccountStatusTests {

                @Test
                @DisplayName("Activation compte - Succès")
                void activateAccount_WithValidPendingClient_ShouldActivateAccount() {
                        // Given
                        String clientId = "client-123";
                        existingClient.setStatus(ClientStatus.PENDING);
                        // CHANGEMENT : repository au lieu de repository
                        when(repository.findById(clientId))
                                        .thenReturn(Optional.of(existingClient));
                        when(repository.save(any(Client.class)))
                                        .thenReturn(existingClient);

                        // When
                        userService.activateAccount(clientId);

                        // Then
                        assertEquals(ClientStatus.ACTIVE, existingClient.getStatus());
                        // CHANGEMENT : repository au lieu de repository
                        verify(repository).save(existingClient);
                }

                @Test
                @DisplayName("Activation compte - Client introuvable")
                void activateAccount_WithInvalidClientId_ShouldThrowException() {
                        // Given
                        String clientId = "invalid-id";
                        // CHANGEMENT : repository au lieu de repository
                        when(repository.findById(clientId))
                                        .thenReturn(Optional.empty());

                        // When & Then
                        BusinessValidationException exception = assertThrows(
                                        BusinessValidationException.class,
                                        () -> userService.activateAccount(clientId));

                        assertEquals("Client introuvable", exception.getMessage());
                }

                @Test
                @DisplayName("Activation compte - Compte déjà traité")
                void activateAccount_WithAlreadyActiveClient_ShouldThrowException() {
                        // Given
                        String clientId = "client-123";
                        existingClient.setStatus(ClientStatus.ACTIVE);
                        // CHANGEMENT : repository au lieu de repository
                        when(repository.findById(clientId))
                                        .thenReturn(Optional.of(existingClient));

                        // When & Then
                        BusinessValidationException exception = assertThrows(
                                        BusinessValidationException.class,
                                        () -> userService.activateAccount(clientId));

                        assertEquals("Compte déjà traité", exception.getMessage());
                }

                @Test
                @DisplayName("Rejet compte - Succès")
                void rejectAccount_WithValidClient_ShouldRejectAccount() {
                        // Given
                        String clientId = "client-123";
                        String reason = "Documents non conformes";
                        // CHANGEMENT : repository au lieu de repository
                        when(repository.findById(clientId))
                                        .thenReturn(Optional.of(existingClient));
                        when(repository.save(any(Client.class)))
                                        .thenReturn(existingClient);

                        // When
                        userService.rejectAccount(clientId, reason);

                        // Then
                        assertEquals(ClientStatus.REJECTED, existingClient.getStatus());
                        // CHANGEMENT : repository au lieu de repository
                        verify(repository).save(existingClient);
                }
        }

        @Nested
        @DisplayName("Tests de statistiques")
        class StatisticsTests {

                @Test
                @DisplayName("Récupération statistiques - Succès")
                void getClientStatistics_ShouldReturnCompleteStats() {
                        // Given
                        when(repository.count()).thenReturn(100L);
                        when(repository.countByStatus(ClientStatus.ACTIVE)).thenReturn(75L);
                        when(repository.countByStatus(ClientStatus.PENDING)).thenReturn(15L);
                        when(repository.countByStatus(ClientStatus.BLOCKED)).thenReturn(10L);
                        when(repository.countNewClientsToday(any(LocalDateTime.class))).thenReturn(5L);

                        // When
                        Map<String, Long> stats = userService.getClientStatistics();

                        // Then
                        assertNotNull(stats);
                        assertEquals(100L, stats.get("total"));
                        assertEquals(75L, stats.get("active"));
                        assertEquals(15L, stats.get("pending"));
                        assertEquals(10L, stats.get("blocked"));
                        assertEquals(5L, stats.get("newToday"));
                }
        }
}
