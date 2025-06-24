package com.serviceAgence.dto.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le rejet d'un document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRejectionRequest {
    
    @NotBlank(message = "Raison du rejet obligatoire")
    @Size(min = 10, max = 500, message = "Raison doit contenir entre 10 et 500 caractères")
    private String reason;
    
    @Size(max = 500, message = "Notes limitées à 500 caractères")
    private String notes; // Notes additionnelles optionnelles
}