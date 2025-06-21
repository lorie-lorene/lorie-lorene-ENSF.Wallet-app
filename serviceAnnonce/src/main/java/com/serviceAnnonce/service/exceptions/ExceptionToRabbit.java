package com.serviceAnnonce.service.exceptions;

import org.springframework.amqp.AmqpAuthenticationException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.serviceAnnonce.pojo.ErrorAndException;

@ControllerAdvice
public class ExceptionToRabbit {
    /* Exception liée au probleme de connectivité */

    @ExceptionHandler({ AmqpConnectException.class })
    public @ResponseBody ErrorAndException handleException(AmqpConnectException exception) {
        return new ErrorAndException(exception.getMessage());
    }

    /* Exception liée au probleme d'echange ave le serveur */

    @ExceptionHandler({ AmqpIOException.class })
    public @ResponseBody ErrorAndException AmqpIOException(AmqpIOException exception) {
        return new ErrorAndException(exception.getMessage());
    }
    /*
     * Exception liée au probleme de configuration,
     * publication ou consommation des messages
     */

    @ExceptionHandler({ AmqpException.class })
    public @ResponseBody ErrorAndException AmqpException(AmqpException exception) {
        return new ErrorAndException(exception.getMessage());
    }
    /* Exception liée au delai d'attente trop long */

    @ExceptionHandler({ AmqpTimeoutException.class })
    public @ResponseBody ErrorAndException AmqpTimeout(AmqpTimeoutException exception) {
        return new ErrorAndException(exception.getMessage());
    }
    /* Exception liée aux entités d'authentification a rabbit */

    @ExceptionHandler({ AmqpAuthenticationException.class })
    public @ResponseBody ErrorAndException AmqpAuthentication(AmqpAuthenticationException exception) {
        return new ErrorAndException(exception.getMessage());
    }
}