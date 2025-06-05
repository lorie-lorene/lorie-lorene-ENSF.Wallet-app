package com.m1_fonda.serviceUser.service.exceptions;

/**
 * Exception levée lors de la validation métier
 * Utilisée pour les erreurs de logique métier (email déjà existant, données invalides, etc.)
 */
public class BusinessValidationException extends RuntimeException {
    
    public BusinessValidationException(String message) {
        super(message);
    }
    
    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}