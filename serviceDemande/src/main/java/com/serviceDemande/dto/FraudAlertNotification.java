package com.serviceDemande.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudAlertNotification {
    private String idClient;
    private String alertType;
    private String details;
    private LocalDateTime timestamp;
}