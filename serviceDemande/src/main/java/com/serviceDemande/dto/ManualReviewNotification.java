package com.serviceDemande.dto;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualReviewNotification {
    private String demandeId;
    private String idClient;
    private Integer riskScore;
    private java.util.List<String> fraudFlags;
    private LocalDateTime createdAt;
}
