package com.serviceAgence.dto.document;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * DTO for bulk document operations (approve/reject)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDocumentRequest {
    
    @NotNull(message = "Liste des IDs de documents requise")
    @NotEmpty(message = "Au moins un document doit être spécifié")
    private List<String> documentIds;
    
    private String comment;  // For approval
    private String reason;   // For rejection
    
    // Constructor for approval
    public BulkDocumentRequest(List<String> documentIds, String comment) {
        this.documentIds = documentIds;
        this.comment = comment;
    }
}