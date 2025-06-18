package com.serviceDemande.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.serviceDemande.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudAnalysisResult {
    private int riskScore;
    private RiskLevel riskLevel;
    private List<String> fraudFlags;
    private boolean requiresManualReview;
    private String recommendation;
    private LocalDateTime analyzedAt;
}

