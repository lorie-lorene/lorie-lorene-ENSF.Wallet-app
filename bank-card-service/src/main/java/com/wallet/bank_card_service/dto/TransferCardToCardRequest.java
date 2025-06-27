package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransferCardToCardRequest {
    @NotBlank
    private String numeroCompteSource;
    
    @NotBlank
    private String idCarteDestination;
    
    @NotNull
    private BigDecimal montant;
    
    private String description;
    
    // Constructeurs
    public TransferCardToCardRequest() {}
    
    public TransferCardToCardRequest(String numeroCompteSource, String idCarteDestination, 
                                   BigDecimal montant, String description) {
        this.numeroCompteSource = numeroCompteSource;
        this.idCarteDestination = idCarteDestination;
        this.montant = montant;
        this.description = description;
    }
    
    // Getters et Setters
    public String getNumeroCompteSource() {
        return numeroCompteSource;
    }
    
    public void setNumeroCompteSource(String numeroCompteSource) {
        this.numeroCompteSource = numeroCompteSource;
    }
    
    public String getIdCarteDestination() {
        return idCarteDestination;
    }
    
    public void setIdCarteDestination(String idCarteDestination) {
        this.idCarteDestination = idCarteDestination;
    }
    
    public BigDecimal getMontant() {
        return montant;
    }
    
    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
