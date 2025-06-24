package com.serviceAgence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour les erreurs de validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private String errorCode;
    private String message;
    private int status;
    private Map<String, String> validationErrors;
    private LocalDateTime timestamp;
}