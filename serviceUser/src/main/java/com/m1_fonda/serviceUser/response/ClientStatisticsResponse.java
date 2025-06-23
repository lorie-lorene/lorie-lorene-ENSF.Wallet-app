package com.m1_fonda.serviceUser.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📊 Client Statistics Response DTO
 * Contains aggregated statistics about clients
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatisticsResponse {
    private long totalClients;
    private long activeClients;
    private long pendingClients;
    private long blockedClients;
    private long rejectedClients;
    private long suspendedClients;
    private long newClientsToday;
    private long newClientsThisWeek;
    private long newClientsThisMonth;
    
    // KYC Statistics
    private long kycVerifiedClients;
    private long kycPendingClients;
    
    // Activity Statistics
    private long clientsLoggedInToday;
    private long clientsLoggedInThisWeek;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;
    
    // Percentage calculations
    public double getActivePercentage() {
        return totalClients > 0 ? (double) activeClients / totalClients * 100 : 0;
    }
    
    public double getKycCompletionRate() {
        return totalClients > 0 ? (double) kycVerifiedClients / totalClients * 100 : 0;
    }
}
