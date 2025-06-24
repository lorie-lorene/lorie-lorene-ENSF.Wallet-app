package com.serviceAgence.dto.document;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'approbation d'un document avec validation faciale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentApprovalRequest {
    
    @Size(max = 500, message = "Notes limitées à 500 caractères")
    private String notes; // Notes générales de l'admin
    
    @Size(max = 300, message = "Notes selfie limitées à 300 caractères")
    private String selfieNotes; // ← NEW: Notes spécifiques au selfie
    
    private Boolean facialMatchConfirmed; // ← NEW: Admin confirme la correspondance faciale
    
    private Boolean livenessConfirmed;    // ← NEW: Admin confirme que c'est une vraie personne
}