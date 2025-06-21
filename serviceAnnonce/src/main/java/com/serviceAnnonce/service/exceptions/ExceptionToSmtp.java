package com.serviceAnnonce.service.exceptions;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionToSmtp {
    //@ExceptionHandler({UnstableConnectionException.class})
    public <UnstableConnectionException> ResponseEntity<String> handleUnstableConnectionException(UnstableConnectionException ex) {
        return ResponseEntity.status(HttpStatus.SC_SERVICE_UNAVAILABLE)
                .body("La connexion au serveur SMTP est instable. Veuillez réessayer plus tard.");
    }
}
