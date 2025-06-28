package com.m1_fonda.serviceUser.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for client summary information
 * Used in paginated client lists
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSummaryDTO {
    
    private String clientId;
    private String cni;
    private String email;
    private String nom;
    private String prenom;
    private String numero;
    private ClientStatus status; // PENDING, ACTIVE, BLOCKED, REJECTED
    private String idAgence;
    private String nomAgence;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Boolean hasAccount;
    private Integer transactionCount;
    private Double accountBalance;
    
    /**
     * Get full name
     */
    public String getFullName() {
        return String.format("%s %s", prenom != null ? prenom : "", nom != null ? nom : "").trim();
    }
    
    /**
     * Check if client is recently registered (within last 7 days)
     */
    public boolean isRecentlyRegistered() {
        if (createdAt == null) return false;
        return createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }
    
    /**
     * Get status display color for UI
     */
    public String getStatusColor() {
        if (status == null) return "gray";
        else if(status == ClientStatus.PENDING) return "yellow";
        else if(status == ClientStatus.ACTIVE) return "green";
        else if(status == ClientStatus.BLOCKED || status == ClientStatus.REJECTED) return "red";
        else return "gray"; // Default color for unknown status
    }
}