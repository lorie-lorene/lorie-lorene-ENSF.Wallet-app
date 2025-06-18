package com.serviceDemande.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.serviceDemande.model.ValidationDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationRequest {
    private String eventId;
    private String idClient;
    private String idAgence;
    private String cni;
    private String email;
    private String nom;
    private String prenom;
    private String numero;
    private String rectoCniHash;
    private String versoCniHash;
    private ValidationDetails agenceValidation;
    private String sourceService;
    private LocalDateTime timestamp;
}
