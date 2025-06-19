package com.wallet.bank_card_service.exception;


import lombok.Getter;

@Getter
public class CarteException extends RuntimeException {
    
    private final String errorCode;
    
    public CarteException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public CarteException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}