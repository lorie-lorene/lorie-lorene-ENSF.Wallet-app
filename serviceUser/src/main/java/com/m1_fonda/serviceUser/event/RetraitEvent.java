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
public class RetraitEvent {
    @JsonProperty("numeroCompte")
    private long numeroCompte;
    
    @JsonProperty("montant")
    private BigDecimal montant; 
    
    @JsonProperty("numeroClient")
    private String numeroClient;
    

    private String eventId = UUID.randomUUID().toString();
    private LocalDateTime timestamp = LocalDateTime.now();
    private String sourceService = "UserService";
}

