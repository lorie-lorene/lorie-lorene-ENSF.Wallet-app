package com.serviceAgence.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgenceStatistics {
    private String idAgence;
    private String nomAgence;
    private Long totalComptes;
    private Long comptesActifs;
    private Long comptesSuspendus;
    private Long comptesBloqués;
    private BigDecimal totalSoldes;
    private Long totalTransactions;
    private BigDecimal totalVolume;
    private BigDecimal capital;
    private BigDecimal soldeDisponible;
    private LocalDateTime generatedAt;
}

