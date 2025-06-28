package com.m1_fonda.serviceUser.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for comprehensive client statistics
 * Used by dashboard to display real client data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientStatisticsDTO {
    
    // Core statistics
    private Long totalClients;
    private Long activeClients;
    private Long pendingClients;
    private Long blockedClients;
    private Long rejectedClients;
    
    // Recent activity
    private Long newClientsToday;
    private Long newClientsThisWeek;
    private Long newClientsThisMonth;
    
    // Status distribution
    private Map<String, Long> statusDistribution;
    
    // Agency distribution
    private Map<String, Long> agencyDistribution;
    
    // Financial statistics
    private Long clientsWithAccounts;
    private Long clientsWithTransactions;
    private Double totalAccountBalance;
    
    // Geographic distribution
    private Map<String, Long> regionDistribution;
    
    // Time-based metrics
    private Map<String, Long> registrationTrends; // Last 6 months
    
    // Metadata
    private LocalDateTime generatedAt;
    private String generatedBy;
    
    /**
     * Calculate percentage of active clients
     */
    public double getActiveClientPercentage() {
        if (totalClients == 0) return 0.0;
        return (double) activeClients / totalClients * 100;
    }
    
    /**
     * Calculate growth rate compared to previous period
     */
    public double getGrowthRate() {
        // This would be calculated based on historical data
        // Implementation depends on your business logic
        return 0.0; // Placeholder
    }
}