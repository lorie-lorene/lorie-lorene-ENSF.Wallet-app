package com.serviceAgence.exception;


import lombok.Getter;

@Getter
public class CompteException extends RuntimeException {
    private final String errorCode;
    
    public CompteException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public CompteException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}