package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CarteStatistiques {
    private String idClient;
    private int totalCartes;
    private int cartesActives;
    private int cartesBloques;
    private BigDecimal soldeTotal;
    private BigDecimal limiteUtiliseeQuotidienne;
    private BigDecimal limiteUtiliseeMensuelle;
    private BigDecimal fraisMensuelsTotal;
    private LocalDateTime prochainPrelevement;
    private LocalDateTime generatedAt;
}
