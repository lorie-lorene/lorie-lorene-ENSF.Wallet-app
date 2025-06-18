package com.serviceAgence.exception;

import lombok.Getter;

@Getter
public class TransactionException extends RuntimeException {
    private final String errorCode;
    
    public TransactionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TransactionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
