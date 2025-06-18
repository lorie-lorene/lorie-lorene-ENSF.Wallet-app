package com.serviceDemande.dto;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualReviewRequest {
    private boolean approved;
    private String notes;
    private String reviewerId;
}