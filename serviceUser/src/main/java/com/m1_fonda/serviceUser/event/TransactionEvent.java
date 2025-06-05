package com.m1_fonda.serviceUser.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TransactionEvent {
    @JsonProperty("numeroComptesend")
    private long numeroCompteSend;
    
    @JsonProperty("montant")
    private BigDecimal montant;
    
    @JsonProperty("numeroComptereceive")
    private long numeroCompteReceive;
    
    
    private String eventId = UUID.randomUUID().toString();
    private LocalDateTime timestamp = LocalDateTime.now();
    private String sourceService = "UserService";
    private String clientId; // Pour traçabilité
}
