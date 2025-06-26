package com.serviceAgence.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
@Data
public class CompteDetails {
    private String numeroCompte;
    private String idClient;
    private String idAgence;
    private BigDecimal solde;
    private String status;
    private LocalDateTime createdAt;
    
    // Constructeurs
    public CompteDetails() {}
    
    public CompteDetails(String numeroCompte, String idClient, String idAgence, 
                        BigDecimal solde, String status) {
        this.numeroCompte = numeroCompte;
        this.idClient = idClient;
        this.idAgence = idAgence;
        this.solde = solde;
        this.status = status;
    }
    
    // Getters et Setters
    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }
    
    public String getIdClient() { return idClient; }
    public void setIdClient(String idClient) { this.idClient = idClient; }
    
    public String getIdAgence() { return idAgence; }
    public void setIdAgence(String idAgence) { this.idAgence = idAgence; }
    
    public BigDecimal getSolde() { return solde; }
    public void setSolde(BigDecimal solde) { this.solde = solde; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
