package com.serviceAgence.dto.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for bulk operation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResult {
    
    private int totalDocuments;
    private int successCount;
    private int failureCount;
    private List<String> successfulDocuments;
    private List<BulkOperationError> errors;
    private String operationType; // "APPROVE" or "REJECT"
    private String performedBy;
    private LocalDateTime completedAt;
    
    /**
     * Inner class for error details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkOperationError {
        private String documentId;
        private String errorMessage;
        private String errorCode;
    }
    
    /**
     * Calculate success rate percentage
     */
    public double getSuccessRate() {
        if (totalDocuments == 0) return 0.0;
        return (double) successCount / totalDocuments * 100;
    }
    
    /**
     * Check if operation was fully successful
     */
    public boolean isFullySuccessful() {
        return failureCount == 0;
    }
    
    /**
     * Check if operation had partial success
     */
    public boolean isPartiallySuccessful() {
        return successCount > 0 && failureCount > 0;
    }
}