package com.serviceAgence.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.serviceAgence.enums.DocumentType;

/**
 * Événement de rejet de document avec détails complets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRejectionEvent {
    private String eventId;
    private String clientId;
    private String documentId;
    private DocumentType documentType;
    private String rejectionReason;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String agenceCode;
    private boolean canResubmit; // Client peut-il resoumettre ?
    private LocalDateTime timestamp;
}