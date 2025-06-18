package com.serviceAgence.exception;

import lombok.Getter;

@Getter
public class AgenceException extends RuntimeException {
    private final String errorCode;
    
    public AgenceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AgenceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}