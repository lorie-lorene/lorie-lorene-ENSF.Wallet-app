package com.serviceDemande.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DemandeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDemandeNotFound(DemandeNotFoundException ex) {
        log.error("Demande introuvable: {}", ex.getMessage());
        
        Map<String, Object> error = createErrorResponse(
            "DEMANDE_INTROUVABLE", 
            ex.getMessage(), 
            HttpStatus.NOT_FOUND
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        log.error("Erreur validation: {}", ex.getMessage());
        
        Map<String, Object> error = createErrorResponse(
            ex.getErrorCode(), 
            ex.getMessage(), 
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(FraudException.class)
    public ResponseEntity<Map<String, Object>> handleFraud(FraudException ex) {
        log.error("Détection fraude: {}", ex.getMessage());
        
        Map<String, Object> error = createErrorResponse(
            "FRAUDE_DETECTEE", 
            ex.getMessage(), 
            HttpStatus.FORBIDDEN
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Erreur runtime: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = createErrorResponse(
            "ERREUR_INTERNE", 
            "Erreur technique interne", 
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", errorCode);
        error.put("message", message);
        error.put("status", status.value());
        error.put("timestamp", LocalDateTime.now());
        return error;
    }
}

// Exceptions personnalisées
class DemandeNotFoundException extends RuntimeException {
    public DemandeNotFoundException(String message) {
        super(message);
    }
}

class ValidationException extends RuntimeException {
    private final String errorCode;
    
    public ValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

class FraudException extends RuntimeException {
    public FraudException(String message) {
        super(message);
    }
}
