package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;

public enum CarteType {
    VIRTUELLE_GRATUITE("Carte Virtuelle Gratuite", BigDecimal.ZERO, new BigDecimal("500000"), new BigDecimal("200000")),
    VIRTUELLE_PREMIUM("Carte Virtuelle Premium", new BigDecimal("5000"), new BigDecimal("2000000"), new BigDecimal("1000000")),
    PHYSIQUE("Carte Physique", new BigDecimal("10000"), new BigDecimal("5000000"), new BigDecimal("2000000"));
    
    private final String libelle;
    private final BigDecimal fraisCreation;
    private final BigDecimal limiteDailyDefault;
    private final BigDecimal limiteMonthlyDefault;
    
    CarteType(String libelle, BigDecimal fraisCreation, BigDecimal limiteDailyDefault, BigDecimal limiteMonthlyDefault) {
        this.libelle = libelle;
        this.fraisCreation = fraisCreation;
        this.limiteDailyDefault = limiteDailyDefault;
        this.limiteMonthlyDefault = limiteMonthlyDefault;
    }
    
    public String getLibelle() { return libelle; }
    public BigDecimal getFraisCreation() { return fraisCreation; }
    public BigDecimal getLimiteDailyDefault() { return limiteDailyDefault; }
    public BigDecimal getLimiteMonthlyDefault() { return limiteMonthlyDefault; }
    
    public boolean isGratuite() {
        return this == VIRTUELLE_GRATUITE;
    }
    
    public boolean isPhysique() {
        return this == PHYSIQUE;
    }
}