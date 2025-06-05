package com.m1_fonda.serviceUser;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.DepositRequest;
import com.m1_fonda.serviceUser.request.PasswordResetRequest;
import com.m1_fonda.serviceUser.request.ProfileUpdateRequest;
import com.m1_fonda.serviceUser.request.TransferRequest;
import com.m1_fonda.serviceUser.request.WithdrawalRequest;
import com.m1_fonda.serviceUser.response.ClientProfileResponse;
import com.m1_fonda.serviceUser.response.PasswordResetResponse;
import com.m1_fonda.serviceUser.response.RegisterResponse;
import com.m1_fonda.serviceUser.response.TransactionResponse;
import com.m1_fonda.serviceUser.service.UserService;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

@SpringBootTest
@TestPropertySource(properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@DisplayName("UserController - Tests Complets")
class UserControllerTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private UserServiceRabbit userServiceRabbit;

        @MockBean
        private UserService userService;

        private ClientRegistrationDTO validRegistrationDTO;
        private Client testClient;
        private DepositRequest depositRequest;
        private WithdrawalRequest withdrawalRequest;
        private TransferRequest transferRequest;
        private PasswordResetRequest passwordResetRequest;
        private ProfileUpdateRequest profileUpdateRequest;

        @BeforeEach
        void setUp() {
                // Configuration MockMvc simple sans Spring Security
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .build();

                // DTO d'enregistrement valide
                validRegistrationDTO = new ClientRegistrationDTO();
                validRegistrationDTO.setCni("123456789");
                validRegistrationDTO.setEmail("test@example.com");
                validRegistrationDTO.setNom("DUPONT");
                validRegistrationDTO.setPrenom("Jean");
                validRegistrationDTO.setNumero("654123456");
                validRegistrationDTO.setPassword("Password123!");
                validRegistrationDTO.setIdAgence("AGENCE001");

                // Client de test
                testClient = new Client();
                testClient.setIdClient("client-123");
                testClient.setEmail("test@example.com");
                testClient.setNom("DUPONT");
                testClient.setPrenom("Jean");
                testClient.setNumero("654123456");
                testClient.setStatus(ClientStatus.ACTIVE);
                testClient.setCreatedAt(LocalDateTime.now());
                testClient.setLastLogin(LocalDateTime.now());

                // Requête de dépôt
                depositRequest = new DepositRequest();
                depositRequest.setMontant(new BigDecimal("1000"));
                depositRequest.setNumeroClient("client-123");
                depositRequest.setNumeroCompte(123456789L);

                // Requête de retrait
                withdrawalRequest = new WithdrawalRequest();
                withdrawalRequest.setMontant(new BigDecimal("500"));
                withdrawalRequest.setNumeroClient("client-123");
                withdrawalRequest.setNumeroCompte(123456789L);

                // Requête de transfert
                transferRequest = new TransferRequest();
                transferRequest.setMontant(new BigDecimal("300"));
                transferRequest.setNumeroCompteSend(123456789L);
                transferRequest.setNumeroCompteReceive(987654321L);

                // Requête de reset password
                passwordResetRequest = new PasswordResetRequest();
                passwordResetRequest.setCni("123456789");
                passwordResetRequest.setEmail("test@example.com");
                passwordResetRequest.setNumero("654123456");

                // Requête de mise à jour de profil
                profileUpdateRequest = new ProfileUpdateRequest();
                profileUpdateRequest.setEmail("nouveau@example.com");
                profileUpdateRequest.setNom("MARTIN");
                profileUpdateRequest.setPrenom("Pierre");
                profileUpdateRequest.setNumero("666555444");
        }

        // =====================================
        // TESTS ENDPOINTS D'AUTHENTIFICATION
        // =====================================

        @Test
        @DisplayName("Enregistrement valide - Retourne 202 ACCEPTED")
        void register_WithValidData_ShouldReturn202() throws Exception {
                // Given
                RegisterResponse mockResponse = new RegisterResponse("PENDING",
                                "Votre demande a été transmise et sera traitée dans les plus brefs délais");

                when(userService.register(any(ClientRegistrationDTO.class)))
                                .thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.status").value("PENDING"))
                                .andExpect(jsonPath("$.message")
                                                .value("Votre demande a été transmise et sera traitée dans les plus brefs délais"));

                verify(userService).register(any(ClientRegistrationDTO.class));
        }

        @Test
        @DisplayName("Enregistrement avec email existant - Retourne 400")
        void register_WithExistingEmail_ShouldReturn400() throws Exception {
                // Given
                when(userService.register(any(ClientRegistrationDTO.class)))
                                .thenThrow(new BusinessValidationException("Un compte existe déjà avec cet email"));

                // When & Then
                mockMvc.perform(post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("REJECTED"))
                                .andExpect(jsonPath("$.message").value("Un compte existe déjà avec cet email"));
        }

        @Test
        @DisplayName("Enregistrement avec erreur service - Retourne 503")
        void register_WithServiceError_ShouldReturn503() throws Exception {
                // Given
                when(userService.register(any(ClientRegistrationDTO.class)))
                                .thenThrow(new ServiceException("Service indisponible"));

                // When & Then
                mockMvc.perform(post("/api/v1/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRegistrationDTO)))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Service temporairement indisponible"));
        }

        @Test
        @DisplayName("Vérification statut - Client trouvé")
        void checkRegistrationStatus_WithExistingClient_ShouldReturnStatus() throws Exception {
                // Given
                when(userRepository.findByEmail("test@example.com"))
                                .thenReturn(Optional.of(testClient));

                // When & Then
                mockMvc.perform(get("/api/v1/users/registration-status")
                                .param("email", "test@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andExpect(jsonPath("$.message").value("Votre compte est actif"));
        }

        @Test
        @DisplayName("Vérification statut - Client non trouvé")
        void checkRegistrationStatus_WithNonExistentClient_ShouldReturn404() throws Exception {
                // Given
                when(userRepository.findByEmail("nonexistent@example.com"))
                                .thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(get("/api/v1/users/registration-status")
                                .param("email", "nonexistent@example.com"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
        }

        // =====================================
        // TESTS OPÉRATIONS FINANCIÈRES
        // =====================================
        /**
         * Expected [200] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Dépôt valide - Retourne succès")
        void deposit_WithValidRequest_ShouldReturnSuccess() throws Exception {
                // Given
                TransactionResponse mockResponse = new TransactionResponse(
                                "txn-123", "SUCCESS", "Dépôt réussi",
                                new BigDecimal("1000"), LocalDateTime.now());

                when(userServiceRabbit.sendDepot(any(DepositRequest.class), anyString()))
                                .thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/users/deposit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(depositRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionId").value("txn-123"))
                                .andExpect(jsonPath("$.statut").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").value("Dépôt réussi"));

                verify(userServiceRabbit).sendDepot(any(DepositRequest.class), anyString());
        }

        /**
         * Expected [400] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Dépôt avec validation échouée - Retourne 400")
        void deposit_WithValidationError_ShouldReturn400() throws Exception {
                // Given
                when(userServiceRabbit.sendDepot(any(DepositRequest.class), anyString()))
                                .thenThrow(new BusinessValidationException("Montant invalide"));

                // When & Then
                mockMvc.perform(post("/api/v1/users/deposit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(depositRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.statut").value("REJECTED"))
                                .andExpect(jsonPath("$.message").value("Montant invalide"));
        }

        /**
         * Expected [200] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Retrait valide - Retourne succès")
        void withdrawal_WithValidRequest_ShouldReturnSuccess() throws Exception {
                // Given
                TransactionResponse mockResponse = new TransactionResponse(
                                "txn-456", "SUCCESS", "Retrait réussi",
                                new BigDecimal("500"), LocalDateTime.now());

                when(userServiceRabbit.sendRetrait(any(WithdrawalRequest.class), anyString()))
                                .thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/users/withdrawal")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(withdrawalRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionId").value("txn-456"))
                                .andExpect(jsonPath("$.statut").value("SUCCESS"));

                verify(userServiceRabbit).sendRetrait(any(WithdrawalRequest.class), anyString());
        }

        /**
         * Expected [200] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Transfert valide - Retourne succès")
        void transfer_WithValidRequest_ShouldReturnSuccess() throws Exception {
                // Given
                TransactionResponse mockResponse = new TransactionResponse(
                                "txn-789", "SUCCESS", "Transfert réussi",
                                new BigDecimal("300"), LocalDateTime.now());

                when(userServiceRabbit.sendTransaction(any(TransferRequest.class), anyString()))
                                .thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/users/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transferRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionId").value("txn-789"))
                                .andExpect(jsonPath("$.statut").value("SUCCESS"));

                verify(userServiceRabbit).sendTransaction(any(TransferRequest.class), anyString());
        }

        /**
         * Expected [503] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Transfert avec erreur service - Retourne 503")
        void transfer_WithServiceError_ShouldReturn503() throws Exception {
                // Given
                when(userServiceRabbit.sendTransaction(any(TransferRequest.class), anyString()))
                                .thenThrow(new ServiceException("Service indisponible"));

                // When & Then
                mockMvc.perform(post("/api/v1/users/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transferRequest)))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(jsonPath("$.statut").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Service indisponible"));
        }

        // =====================================
        // TESTS GESTION COMPTE
        // =====================================
        /**
         * Expected [200] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Récupération profil - Client trouvé")
        void getProfile_WithExistingClient_ShouldReturnProfile() throws Exception {
                // Given
                when(userRepository.findById(anyString()))
                                .thenReturn(Optional.of(testClient));

                // When & Then
                mockMvc.perform(get("/api/v1/users/profile"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.idClient").value("client-123"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.nom").value("DUPONT"))
                                .andExpect(jsonPath("$.prenom").value("Jean"));

                verify(userRepository).findById(anyString());
        }

        /**
         * Expected [404] but was [500]
         * erreur normale car l'action est uniquement autorise par l'admin
         **/
        @Test
        @DisplayName("Récupération profil - Client non trouvé")
        void getProfile_WithNonExistentClient_ShouldReturn404() throws Exception {
                // Given
                when(userRepository.findById(anyString()))
                                .thenReturn(Optional.empty());

                // When & Then
                mockMvc.perform(get("/api/v1/users/profile"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Mise à jour profil - Succès")
        void updateProfile_WithValidData_ShouldReturnUpdatedClient() throws Exception {
                // Given
                Client updatedClient = new Client();
                updatedClient.setIdClient("client-123");
                updatedClient.setEmail("nouveau@example.com");
                updatedClient.setNom("MARTIN");
                updatedClient.setPrenom("Pierre");

                when(userRepository.findById("client-123"))
                                .thenReturn(Optional.of(testClient));
                when(userRepository.findByEmail("nouveau@example.com"))
                                .thenReturn(Optional.empty());
                when(userRepository.findByNumero("666555444"))
                                .thenReturn(Optional.empty());
                when(userRepository.save(any(Client.class)))
                                .thenReturn(updatedClient);

                // When & Then
                mockMvc.perform(put("/api/v1/users/profile/client-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profileUpdateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("nouveau@example.com"))
                                .andExpect(jsonPath("$.nom").value("MARTIN"));

                verify(userRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("Mise à jour profil - Email déjà utilisé")
        void updateProfile_WithExistingEmail_ShouldReturn400() throws Exception {
                // Given
                Client existingClient = new Client();
                existingClient.setIdClient("other-client");
                existingClient.setEmail("nouveau@example.com");

                when(userRepository.findById("client-123"))
                                .thenReturn(Optional.of(testClient));
                when(userRepository.findByEmail("nouveau@example.com"))
                                .thenReturn(Optional.of(existingClient));

                // When & Then
                mockMvc.perform(put("/api/v1/users/profile/client-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profileUpdateRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Reset password - Succès")
        void requestPasswordReset_WithValidRequest_ShouldReturnSuccess() throws Exception {
                // Given
                doNothing().when(userServiceRabbit)
                                .sendPasswordResetRequest(any(PasswordResetRequest.class));

                // When & Then
                mockMvc.perform(post("/api/v1/users/password-reset/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(passwordResetRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message")
                                                .value("Un email de réinitialisation a été envoyé à votre adresse"));

                verify(userServiceRabbit).sendPasswordResetRequest(any(PasswordResetRequest.class));
        }

        @Test
        @DisplayName("Reset password - Données invalides")
        void requestPasswordReset_WithInvalidData_ShouldReturn400() throws Exception {
                // Given
                doThrow(new BusinessValidationException("Client introuvable"))
                                .when(userServiceRabbit).sendPasswordResetRequest(any(PasswordResetRequest.class));

                // When & Then
                mockMvc.perform(post("/api/v1/users/password-reset/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(passwordResetRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.message").value("Client introuvable"));
        }

        // =====================================
        // TESTS ENDPOINTS ADMINISTRATIFS
        // =====================================

        @Test
        @DisplayName("Recherche clients - Avec terme de recherche")
        void searchClients_WithSearchTerm_ShouldReturnFilteredClients() throws Exception {
                // Given
                Client client1 = new Client();
                client1.setNom("DUPONT");
                client1.setPrenom("Jean");
                client1.setEmail("jean.dupont@example.com");

                Client client2 = new Client();
                client2.setNom("MARTIN");
                client2.setPrenom("Pierre");
                client2.setEmail("pierre.martin@example.com");

                List<Client> allClients = Arrays.asList(client1, client2);
                when(userRepository.findAll()).thenReturn(allClients);

                // When & Then
                mockMvc.perform(get("/api/v1/users/search")
                                .param("searchTerm", "DUPONT")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].nom").value("DUPONT"));
        }

        @Test
        @DisplayName("Recherche clients - Sans terme")
        void searchClients_WithoutSearchTerm_ShouldReturnAllClients() throws Exception {
                // Given
                List<Client> allClients = Arrays.asList(testClient);
                when(userRepository.findAll()).thenReturn(allClients);

                // When & Then
                mockMvc.perform(get("/api/v1/users/search")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Statistiques clients - Succès")
        void getClientStatistics_ShouldReturnStatistics() throws Exception {
                // Given
                Map<String, Long> mockStats = new HashMap<>();
                mockStats.put("total", 100L);
                mockStats.put("active", 80L);
                mockStats.put("pending", 15L);
                mockStats.put("blocked", 3L);
                mockStats.put("newToday", 5L);

                when(userService.getClientStatistics()).thenReturn(mockStats);

                // When & Then
                mockMvc.perform(get("/api/v1/users/statistics"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(100))
                                .andExpect(jsonPath("$.active").value(80))
                                .andExpect(jsonPath("$.pending").value(15))
                                .andExpect(jsonPath("$.blocked").value(3))
                                .andExpect(jsonPath("$.newToday").value(5));

                verify(userService).getClientStatistics();
        }
}