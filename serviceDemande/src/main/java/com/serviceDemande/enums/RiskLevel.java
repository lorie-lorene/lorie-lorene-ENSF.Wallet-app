package com.serviceDemande.enums;


import lombok.Getter;

@Getter
public enum RiskLevel {
    LOW("Risque faible", 0, 30),
    MEDIUM("Risque moyen", 31, 60),
    HIGH("Risque élevé", 61, 80),
    CRITICAL("Risque critique", 81, 100);

    private final String description;
    private final int minScore;
    private final int maxScore;

    RiskLevel(String description, int minScore, int maxScore) {
        this.description = description;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public static RiskLevel fromScore(int score) {
        for (RiskLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return CRITICAL;
    }
}
