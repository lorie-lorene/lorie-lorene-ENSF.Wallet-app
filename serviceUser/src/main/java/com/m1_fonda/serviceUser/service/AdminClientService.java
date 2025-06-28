package com.m1_fonda.serviceUser.service;

import com.m1_fonda.serviceUser.pojo.ClientStatisticsDTO;
import com.m1_fonda.serviceUser.pojo.ClientSummaryDTO;
import com.m1_fonda.serviceUser.pojo.ClientDetailDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 📊 Admin Client Service for ServiceUser
 * 
 * Provides administrative functions for client management and statistics
 * Uses UserRepository to manage Client entities from the model package
 * Used by AgenceService dashboard to fetch real client data
 */
@Service
@Slf4j
public class AdminClientService {

    @Autowired
    private UserRepository userRepository;  // Using UserRepository instead of ClientRepository

    /**
     * 📊 Generate comprehensive client statistics
     * This provides real data for the dashboard overview
     */
    public ClientStatisticsDTO getClientStatistics() {
        log.info("📊 Generating comprehensive client statistics...");
        
        try {
            // Get basic counts
            long totalClients = userRepository.count();
            long activeClients = userRepository.countByStatus(ClientStatus.ACTIVE);
            long pendingClients = userRepository.countByStatus(ClientStatus.PENDING);
            long blockedClients = userRepository.countByStatus(ClientStatus.BLOCKED);
            long rejectedClients = userRepository.countByStatus(ClientStatus.REJECTED);
            
            // Recent activity
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = today.minusDays(7);
            LocalDateTime monthStart = today.minusDays(30);
            
            long newClientsToday = userRepository.countByCreatedAtAfter(today);
            long newClientsThisWeek = userRepository.countByCreatedAtAfter(weekStart);
            long newClientsThisMonth = userRepository.countByCreatedAtAfter(monthStart);
            
            // Status distribution
            Map<String, Long> statusDistribution = Arrays.stream(ClientStatus.values())
                    .collect(Collectors.toMap(
                            Enum::name,
                            status -> userRepository.countByStatus(status)
                    ));
            
            // Agency distribution
            Map<String, Long> agencyDistribution = getAgencyDistribution();
            
            // Registration trends (last 6 months)
            Map<String, Long> registrationTrends = getRegistrationTrends();
            
            // Financial statistics (if you have account integration)
            long clientsWithAccounts = getClientsWithAccountsCount();
            long clientsWithTransactions = getClientsWithTransactionsCount();
            double totalAccountBalance = getTotalAccountBalance();
            
            // Build statistics DTO
            ClientStatisticsDTO statistics = ClientStatisticsDTO.builder()
                    .totalClients(totalClients)
                    .activeClients(activeClients)
                    .pendingClients(pendingClients)
                    .blockedClients(blockedClients)
                    .rejectedClients(rejectedClients)
                    .newClientsToday(newClientsToday)
                    .newClientsThisWeek(newClientsThisWeek)
                    .newClientsThisMonth(newClientsThisMonth)
                    .statusDistribution(statusDistribution)
                    .agencyDistribution(agencyDistribution)
                    .registrationTrends(registrationTrends)
                    .clientsWithAccounts(clientsWithAccounts)
                    .clientsWithTransactions(clientsWithTransactions)
                    .totalAccountBalance(totalAccountBalance)
                    .generatedAt(LocalDateTime.now())
                    .generatedBy("AdminClientService")
                    .build();
            
            log.info("📊 Statistics generated: {} total clients, {} active, {} pending", 
                    totalClients, activeClients, pendingClients);
            
            return statistics;
            
        } catch (Exception e) {
            log.error("❌ Error generating client statistics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate client statistics", e);
        }
    }

    /**
     * 👥 Get paginated list of all clients
     */
    public Page<ClientSummaryDTO> getAllClients(Pageable pageable, String status, 
                                               String search, String agenceId) {
        log.info("👥 Fetching clients - page: {}, status: {}, search: {}, agence: {}", 
                pageable.getPageNumber(), status, search, agenceId);
        
        try {
            Page<Client> clientsPage;
            
            // Apply filters
            if (status != null && search != null && agenceId != null) {
                clientsPage = userRepository.findByStatusAndSearchAndAgence(
                        ClientStatus.valueOf(status.toUpperCase()), search, agenceId, pageable);
            } else if (status != null && search != null) {
                clientsPage = userRepository.findByStatusAndEmailContainingIgnoreCaseOrNomContainingIgnoreCase(
                        ClientStatus.valueOf(status.toUpperCase()), search, search, pageable);
            } else if (status != null) {
                clientsPage = userRepository.findByStatus(ClientStatus.valueOf(status.toUpperCase()), pageable);
            } else if (search != null) {
                clientsPage = userRepository.findByEmailContainingIgnoreCaseOrNomContainingIgnoreCase(
                        search, search, pageable);
            } else if (agenceId != null) {
                clientsPage = userRepository.findByIdAgence(agenceId, pageable);
            } else {
                clientsPage = userRepository.findAll(pageable);
            }
            
            // Convert to DTOs
            return clientsPage.map(this::convertToClientSummary);
            
        } catch (Exception e) {
            log.error("❌ Error fetching clients: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch clients", e);
        }
    }

    /**
     * 🔍 Search clients by various criteria
     */
    public Page<ClientSummaryDTO> searchClients(String query, Pageable pageable) {
        log.info("🔍 Searching clients with query: {}", query);
        
        try {
            // Perform multi-field search
            Page<Client> results = userRepository.findByEmailContainingIgnoreCaseOrNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrCniContainingOrNumeroContaining(
                    query, query, query, query, query, pageable);
            
            return results.map(this::convertToClientSummary);
            
        } catch (Exception e) {
            log.error("❌ Error searching clients: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search clients", e);
        }
    }

    /**
     * 📈 Get statistics by agency
     */
    public Map<String, Object> getStatisticsByAgency() {
        log.info("📈 Generating statistics by agency...");
        
        try {
            // Get agency distribution
            Map<String, Long> agencyDistribution = getAgencyDistribution();
            
            // Calculate percentages
            long totalClients = userRepository.count();
            Map<String, Double> agencyPercentages = agencyDistribution.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> totalClients > 0 ? (entry.getValue().doubleValue() / totalClients * 100) : 0.0
                    ));
            
            // Get top performing agencies
            List<Map<String, Object>> topAgencies = agencyDistribution.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("agencyId", entry.getKey());
                        map.put("clientCount", entry.getValue());
                        map.put("percentage", agencyPercentages.get(entry.getKey()));
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return Map.of(
                    "distribution", agencyDistribution,
                    "percentages", agencyPercentages,
                    "topAgencies", topAgencies,
                    "totalAgencies", agencyDistribution.size(),
                    "generatedAt", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("❌ Error generating agency statistics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate agency statistics", e);
        }
    }

    /**
     * 🕒 Get recent client activity
     */
    public Map<String, Object> getRecentActivity() {
        log.info("🕒 Fetching recent client activity...");
        
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
            LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
            
            // Recent registrations
            List<Client> recentRegistrations = userRepository.findTop10ByCreatedAtAfterOrderByCreatedAtDesc(last24Hours);
            
            // Recent logins (if you track this)
            List<Map<String, Object>> recentLogins = getRecentLogins();
            
            // Recent status changes
            List<Map<String, Object>> recentStatusChanges = getRecentStatusChanges();
            
            return Map.of(
                    "recentRegistrations", recentRegistrations.stream()
                            .map(this::convertToClientSummary)
                            .collect(Collectors.toList()),
                    "recentLogins", recentLogins,
                    "recentStatusChanges", recentStatusChanges,
                    "registrationsLast24h", recentRegistrations.size(),
                    "registrationsLastWeek", userRepository.countByCreatedAtAfter(lastWeek),
                    "generatedAt", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("❌ Error fetching recent activity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch recent activity", e);
        }
    }

    // =====================================
    // PRIVATE HELPER METHODS
    // =====================================

    /**
     * Convert Client entity to ClientSummaryDTO
     */
    private ClientSummaryDTO convertToClientSummary(Client client) {
        return ClientSummaryDTO.builder()
                .clientId(client.getIdClient())
                .cni(client.getCni())
                .email(client.getEmail())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .numero(client.getNumero())
                .status(client.getStatus()) // Using ClientStatus enum directly
                .idAgence(client.getIdAgence())
                .nomAgence(getAgencyName(client.getIdAgence())) // Implement this
                .createdAt(client.getCreatedAt())
                .lastLoginAt(client.getLastLogin()) // If you track this
                .hasAccount(hasAccount(client.getIdClient())) // Implement this
                .transactionCount(getTransactionCount(client.getIdClient())) // Implement this
                .accountBalance(getAccountBalance(client.getIdClient())) // Implement this
                .build();
    }

    /**
     * Get agency distribution
     */
    private Map<String, Long> getAgencyDistribution() {
        return userRepository.findAll().stream()
                .filter(client -> client.getIdAgence() != null)
                .collect(Collectors.groupingBy(
                        Client::getIdAgence,
                        Collectors.counting()
                ));
    }

    /**
     * Get registration trends for the last 6 months
     */
    private Map<String, Long> getRegistrationTrends() {
        Map<String, Long> trends = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            long count = userRepository.countByCreatedAtBetween(monthStart, monthEnd);
            String monthKey = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            trends.put(monthKey, count);
        }
        
        return trends;
    }

    /**
     * Placeholder methods - implement based on your system integration
     */
    private long getClientsWithAccountsCount() {
        // Implement based on your account service integration
        return userRepository.countByStatus(ClientStatus.ACTIVE); // Placeholder
    }

    private long getClientsWithTransactionsCount() {
        // Implement based on your transaction service integration
        return userRepository.countByStatus(ClientStatus.ACTIVE) / 2; // Placeholder
    }

    private double getTotalAccountBalance() {
        // Implement based on your account service integration
        return 0.0; // Placeholder
    }

    private String getAgencyName(String agencyId) {
        // Implement based on your agency service integration
        return "Agency " + agencyId; // Placeholder
    }

    private Boolean hasAccount(String clientId) {
        // Implement based on your account service integration
        return true; // Placeholder
    }

    private Integer getTransactionCount(String clientId) {
        // Implement based on your transaction service integration
        return 0; // Placeholder
    }

    private Double getAccountBalance(String clientId) {
        // Implement based on your account service integration
        return 0.0; // Placeholder
    }

    private List<Map<String, Object>> getRecentLogins() {
        // Implement based on your login tracking
        return new ArrayList<>(); // Placeholder
    }

    private List<Map<String, Object>> getRecentStatusChanges() {
        // Implement based on your audit log
        return new ArrayList<>(); // Placeholder
    }
}