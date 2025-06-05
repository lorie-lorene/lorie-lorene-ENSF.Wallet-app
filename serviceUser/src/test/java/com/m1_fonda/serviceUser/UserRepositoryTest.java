package com.m1_fonda.serviceUser;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;

@DataMongoTest
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private Client activeClient;
    private Client pendingClient;
    private Client blockedClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Client actif
        activeClient = createTestClient("active@test.com", "123456789", "654111111",
                ClientStatus.ACTIVE, 0);
        activeClient.setLastLogin(LocalDateTime.now().minusDays(1));

        // Client en attente
        pendingClient = createTestClient("pending@test.com", "987654321", "654222222",
                ClientStatus.PENDING, 0);

        // Client bloqué avec tentatives
        blockedClient = createTestClient("blocked@test.com", "456789123", "654333333",
                ClientStatus.BLOCKED, 5);
        blockedClient.setLastFailedLogin(LocalDateTime.now().minusHours(1));

        userRepository.saveAll(List.of(activeClient, pendingClient, blockedClient));
    }

    private Client createTestClient(String email, String cni, String numero,
            ClientStatus status, int loginAttempts) {
        Client client = new Client();
        client.setEmail(email);
        client.setCni(cni);
        client.setNumero(numero);
        client.setNom("DUPONT");
        client.setPrenom("Jean");
        client.setPasswordHash("hashedPassword");
        client.setSalt("salt123");
        client.setStatus(status);
        client.setLoginAttempts(loginAttempts);
        client.setCreatedAt(LocalDateTime.now());
        client.setIdAgence("AGENCE001");
        return client;
    }

    @Nested
    @DisplayName("Tests de recherche basique")
    class BasicSearchTests {

        @Test
        @DisplayName("Recherche par email - Insensible à la casse")
        void findByEmail_WithDifferentCase_ShouldReturnClient() {
            // When
            Optional<Client> result = userRepository.findByEmail("ACTIVE@TEST.COM");

            // Then
            assertTrue(result.isPresent());
            assertEquals("active@test.com", result.get().getEmail());
        }

        @Test
        @DisplayName("Recherche par CNI - Succès")
        void findByCni_WithValidCNI_ShouldReturnClient() {
            // When
            Optional<Client> result = userRepository.findByCni("123456789");

            // Then
            assertTrue(result.isPresent());
            assertEquals(activeClient.getEmail(), result.get().getEmail());
        }

        @Test
        @DisplayName("Recherche par numéro - Succès")
        void findByNumero_WithValidNumber_ShouldReturnClient() {
            // When
            Optional<Client> result = userRepository.findByNumero("654111111");

            // Then
            assertTrue(result.isPresent());
            assertEquals(activeClient.getEmail(), result.get().getEmail());
        }

        @Test
        @DisplayName("Recherche avec données inexistantes - Retourne vide")
        void findByEmail_WithNonExistentEmail_ShouldReturnEmpty() {
            // When
            Optional<Client> result = userRepository.findByEmail("nonexistent@test.com");

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Tests de recherche de clients actifs")
    class ActiveClientTests {

        @Test
        @DisplayName("Recherche client actif par numéro - Succès")
        void findActiveClientByNumero_WithActiveClient_ShouldReturnClient() {
            // When
            Optional<Client> result = userRepository.findActiveClientByNumero("654111111");

            // Then
            assertTrue(result.isPresent());
            assertEquals(ClientStatus.ACTIVE, result.get().getStatus());
        }

        @Test
        @DisplayName("Recherche client actif par numéro - Client non actif")
        void findActiveClientByNumero_WithBlockedClient_ShouldReturnEmpty() {
            // When
            Optional<Client> result = userRepository.findActiveClientByNumero("654333333");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Recherche client actif par email - Succès")
        void findActiveClientByEmail_WithActiveClient_ShouldReturnClient() {
            // When
            Optional<Client> result = userRepository.findActiveClientByEmail("active@test.com");

            // Then
            assertTrue(result.isPresent());
            assertEquals(ClientStatus.ACTIVE, result.get().getStatus());
        }
    }

    @Nested
    @DisplayName("Tests de requêtes métier")
    class BusinessQueryTests {

        @Test
        @DisplayName("Recherche par statut - Multiples résultats")
        void findByStatus_ShouldReturnClientsWithCorrectStatus() {
            // When
            List<Client> activeClients = userRepository.findByStatus(ClientStatus.ACTIVE);
            List<Client> pendingClients = userRepository.findByStatus(ClientStatus.PENDING);

            // Then
            assertEquals(1, activeClients.size());
            assertEquals(1, pendingClients.size());
            assertEquals(ClientStatus.ACTIVE, activeClients.get(0).getStatus());
            assertEquals(ClientStatus.PENDING, pendingClients.get(0).getStatus());
        }

        @Test
        @DisplayName("Recherche par agence")
        void findByAgence_ShouldReturnClientsFromSameAgency() {
            // When
            List<Client> agencyClients = userRepository.findByAgence("AGENCE001");

            // Then
            assertEquals(3, agencyClients.size()); // Tous nos clients de test
            agencyClients.forEach(client -> assertEquals("AGENCE001", client.getIdAgence()));
        }

        @Test
        @DisplayName("Recherche clients avec tentatives échouées")
        void findClientsWithFailedAttempts_ShouldReturnCorrectClients() {
            // When
            List<Client> clientsWithFailures = userRepository.findClientsWithFailedAttempts(3);

            // Then
            assertEquals(1, clientsWithFailures.size());
            assertEquals(blockedClient.getEmail(), clientsWithFailures.get(0).getEmail());
            assertTrue(clientsWithFailures.get(0).getLoginAttempts() >= 3);
        }

        @Test
        @DisplayName("Recherche clients créés dans une période")
        void findByCreatedAtBetween_ShouldReturnClientsInDateRange() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);

            // When
            List<Client> recentClients = userRepository.findByCreatedAtBetween(start, end);

            // Then
            assertEquals(3, recentClients.size()); // Tous créés aujourd'hui
        }

        @Test
        @DisplayName("Recherche clients inactifs")
        void findInactiveClients_ShouldReturnClientsWithOldLastLogin() {
            // Given - Créer un client avec ancienne connexion
            Client oldClient = createTestClient("old@test.com", "111222333", "654444444",
                    ClientStatus.ACTIVE, 0);
            oldClient.setLastLogin(LocalDateTime.now().minusDays(30));
            userRepository.save(oldClient);

            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

            // When
            List<Client> inactiveClients = userRepository.findInactiveClients(cutoff);

            // Then
            assertEquals(1, inactiveClients.size());
            assertEquals("old@test.com", inactiveClients.get(0).getEmail());
        }
    }

    @Nested
    @DisplayName("Tests de comptage")
    class CountingTests {

        @Test
        @DisplayName("Compter par statut")
        void countByStatus_ShouldReturnCorrectCounts() {
            // When
            long activeCount = userRepository.countByStatus(ClientStatus.ACTIVE);
            long pendingCount = userRepository.countByStatus(ClientStatus.PENDING);
            long blockedCount = userRepository.countByStatus(ClientStatus.BLOCKED);

            // Then
            assertEquals(1, activeCount);
            assertEquals(1, pendingCount);
            assertEquals(1, blockedCount);
        }

        @Test
        @DisplayName("Compter nouveaux clients du jour")
        void countNewClientsToday_ShouldReturnTodaysClients() {
            // Given
            LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);

            // When
            long todayCount = userRepository.countNewClientsToday(startOfDay);

            // Then
            assertEquals(3, todayCount); // Tous créés aujourd'hui
        }

        @Test
        @DisplayName("Recherche clients bloqués ou suspendus")
        void findBlockedOrSuspendedClients_ShouldReturnCorrectClients() {
            // Given - Ajouter un client suspendu
            Client suspendedClient = createTestClient("suspended@test.com", "777888999",
                    "654555555", ClientStatus.SUSPENDED, 0);
            userRepository.save(suspendedClient);

            // When
            List<Client> blockedOrSuspended = userRepository.findBlockedOrSuspendedClients();

            // Then
            assertEquals(2, blockedOrSuspended.size());
            assertTrue(blockedOrSuspended.stream()
                    .anyMatch(c -> c.getStatus() == ClientStatus.BLOCKED));
            assertTrue(blockedOrSuspended.stream()
                    .anyMatch(c -> c.getStatus() == ClientStatus.SUSPENDED));
        }
    }

    @Nested
    @DisplayName("Tests de vérification d'existence")
    class ExistenceTests {

        @Test
        @DisplayName("Vérifier existence par email - Existant")
        void existsByEmail_WithExistingEmail_ShouldReturnTrue() {
            // When
            boolean exists = userRepository.existsByEmail("active@test.com");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("Vérifier existence par email - Non existant")
        void existsByEmail_WithNonExistentEmail_ShouldReturnFalse() {
            // When
            boolean exists = userRepository.existsByEmail("nonexistent@test.com");

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("Vérifier existence par CNI")
        void existsByCni_WithExistingCNI_ShouldReturnTrue() {
            // When
            boolean exists = userRepository.existsByCni("123456789");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("Vérifier existence par numéro")
        void existsByNumero_WithExistingNumber_ShouldReturnTrue() {
            // When
            boolean exists = userRepository.existsByNumero("654111111");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("Vérifier existence client actif par numéro - Client actif")
        void existsActiveClientByNumero_WithActiveClient_ShouldReturnTrue() {
            // When
            boolean exists = userRepository.existsActiveClientByNumero("654111111");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("Vérifier existence client actif par numéro - Client non actif")
        void existsActiveClientByNumero_WithBlockedClient_ShouldReturnFalse() {
            // When
            boolean exists = userRepository.existsActiveClientByNumero("654333333");

            // Then
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Tests de sécurité et audit")
    class SecurityAuditTests {

        @Test
        @DisplayName("Recherche activité suspecte")
        void findSuspiciousActivity_ShouldReturnClientsWithRecentFailures() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(2);

            // When
            List<Client> suspicious = userRepository.findSuspiciousActivity(since);

            // Then
            assertEquals(1, suspicious.size());
            assertEquals(blockedClient.getEmail(), suspicious.get(0).getEmail());
            assertTrue(suspicious.get(0).getLoginAttempts() >= 3);
        }

        @Test
        @DisplayName("Recherche clients nécessitant changement de mot de passe")
        void findClientsNeedingPasswordChange_ShouldReturnClientsWithOldPasswords() {
            // Given - Créer client avec ancien mot de passe
            Client oldPasswordClient = createTestClient("oldpass@test.com", "555666777",
                    "654666666", ClientStatus.ACTIVE, 0);
            oldPasswordClient.setPasswordChangedAt(LocalDateTime.now().minusDays(90));
            userRepository.save(oldPasswordClient);

            LocalDateTime cutoff = LocalDateTime.now().minusDays(60);

            // When
            List<Client> needingChange = userRepository.findClientsNeedingPasswordChange(cutoff);

            // Then
            assertEquals(1, needingChange.size());
            assertEquals("oldpass@test.com", needingChange.get(0).getEmail());
        }
    }
}