package com.serviceAgence.exception;


import lombok.Getter;

@Getter
public class KYCValidationException extends RuntimeException {
    private final String errorCode;
    
    public KYCValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public KYCValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}