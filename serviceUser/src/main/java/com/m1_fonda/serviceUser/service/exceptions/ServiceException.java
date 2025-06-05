package com.m1_fonda.serviceUser.service.exceptions;

/**
 * Exception technique levée lors d'erreurs de service
 * Utilisée pour les erreurs techniques (RabbitMQ down, MongoDB inaccessible, etc.)
 */
public class ServiceException extends RuntimeException {
    
    public ServiceException(String message) {
        super(message);
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
