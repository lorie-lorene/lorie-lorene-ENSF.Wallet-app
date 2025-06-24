package com.wallet.bank_card_service.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CarteWithdrawalResult {
    private boolean success;
    private String requestId;
    private String idCarte;
    private BigDecimal montantDemande;
    private BigDecimal fraisRetrait;
    private BigDecimal montantNet;
    private BigDecimal nouveauSoldeCarte;
    private String provider;
    private String numeroTelephone;
    private String status;
    private String message;
    private LocalDateTime timestamp;
    
    public static CarteWithdrawalResult success(String requestId, String idCarte, 
            BigDecimal montantDemande, BigDecimal frais, BigDecimal nouveauSolde, 
            String provider, String message) {
        CarteWithdrawalResult result = new CarteWithdrawalResult();
        result.success = true;
        result.requestId = requestId;
        result.idCarte = idCarte;
        result.montantDemande = montantDemande;
        result.fraisRetrait = frais;
        result.montantNet = montantDemande.subtract(frais);
        result.nouveauSoldeCarte = nouveauSolde;
        result.provider = provider;
        result.status = "PENDING";
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
    
    public static CarteWithdrawalResult failed(String message) {
        CarteWithdrawalResult result = new CarteWithdrawalResult();
        result.success = false;
        result.status = "FAILED";
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
}