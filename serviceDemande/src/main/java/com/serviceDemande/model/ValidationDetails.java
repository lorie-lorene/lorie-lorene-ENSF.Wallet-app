package com.serviceDemande.model;
import java.time.LocalDateTime;
import java.util.List;

import com.serviceDemande.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationDetails {
    private boolean kycValid;
    private boolean documentsValid;
    private boolean formatValid;
    private Integer qualityScore;
    private List<String> validationNotes;
    private LocalDateTime validatedAt;
    private String validatedBy;
}