package com.m1_fonda.serviceUser.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for detailed client information
 * Used when viewing individual client details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDetailDTO {
    
    // Basic information
    private String clientId;
    private String cni;
    private String email;
    private String nom;
    private String prenom;
    private String numero;
    private String status;
    
    // Agency information
    private String idAgence;
    private String nomAgence;
    
    // Registration details
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String registrationNotes;
    
    // Login activity
    private LocalDateTime lastLoginAt;
    private Integer loginCount;
    private String lastLoginIp;
    
    // Account information
    private Boolean hasAccount;
    private String accountNumber;
    private Double accountBalance;
    private LocalDateTime accountCreatedAt;
    
    // Transaction summary
    private Integer totalTransactions;
    private Double totalTransactionAmount;
    private LocalDateTime lastTransactionAt;
    
    // Documents
    private List<Map<String, Object>> documents;
    
    // Security information
    private Boolean twoFactorEnabled;
    private LocalDateTime passwordLastChanged;
    private Integer failedLoginAttempts;
    
    // Audit trail
    private List<Map<String, Object>> auditLog;
    
    /**
     * Check if client account is fully set up
     */
    public boolean isFullySetUp() {
        return hasAccount != null && hasAccount && 
               status != null && "ACTIVE".equals(status.toUpperCase());
    }
    
    /**
     * Get risk level based on activity
     */
    public String getRiskLevel() {
        if (failedLoginAttempts != null && failedLoginAttempts > 5) return "HIGH";
        if (lastLoginAt != null && lastLoginAt.isBefore(LocalDateTime.now().minusDays(30))) return "MEDIUM";
        return "LOW";
    }
}