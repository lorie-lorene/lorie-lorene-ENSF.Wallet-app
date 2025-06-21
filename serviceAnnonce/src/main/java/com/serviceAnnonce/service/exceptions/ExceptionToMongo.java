
package com.serviceAnnonce.service.exceptions;

import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.mongodb.core.MongoDataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import java.net.ConnectException;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoNodeIsRecoveringException;
import com.mongodb.MongoQueryException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteException;
import com.serviceAnnonce.pojo.ErrorAndException;

@ControllerAdvice
public class ExceptionToMongo {

    /* Exception liée au path pour l'id */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({ EmptyResultDataAccessException.class })
    public @ResponseBody ErrorAndException handleException(EmptyResultDataAccessException exception) {
        return new ErrorAndException(exception.getMessage());
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({ NullPointerException.class })
    public @ResponseBody ErrorAndException NullPointerException(NullPointerException exception) {
        return new ErrorAndException("tontine indisponible");
    }

    /* Exception liée au path pour l'url */
    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler({ NoResourceFoundException.class })
    public @ResponseBody ErrorAndException NoResourceFoundException(NoResourceFoundException exception) {
        return new ErrorAndException("aucun chemin ne correspond");
    }

    /* Exception liée a une erreur sur une donnée */
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ MongoDataIntegrityViolationException.class })
    public @ResponseBody ErrorAndException mongoDataIntegrityViolationException(
            MongoDataIntegrityViolationException exception) {
        return new ErrorAndException("violation d'une donnée");
    }

    /* Exception liée aux problemes sur les requetes */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({ MongoQueryException.class })
    public @ResponseBody ErrorAndException mongoQueryException(MongoQueryException exception) {
        return new ErrorAndException("probleme d'execution de la requete");
    }

    /* Exception liée aux erreurs pendant une modification dans la bd */
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ MongoWriteException.class })
    public @ResponseBody ErrorAndException mongoWriteException(MongoWriteException exception) {
        return new ErrorAndException("erreur lors de la modification dans la base de donnée");
    }

    /* Exception liée a la communication avec la bd */
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ RuntimeException.class })
    public @ResponseBody ErrorAndException mongoSocketException(RuntimeException exception) {

        return new ErrorAndException("communication echouée");
    }
    /* Exception liée a la communication avec la bd */
    // @ResponseStatus(SERVICE_UNAVAILABLE)
    // @ExceptionHandler({MongoSocketException.class})
    // public @ResponseBody ErrorAndException
    // mongoSocketException(MongoSocketException exception){

    // return new ErrorAndException("communication echouée");
    // }
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ MongoSocketException.class })
    public @ResponseBody ErrorAndException mongoSocketException2(MongoSocketException exception) {

        if (exception instanceof MongoSocketException) {
            MongoSocketException socketException = (MongoSocketException) exception;
            if (socketException.getMessage().contains("TIMEOUT")) {
                throw new ServerDownException("Le serveur est éteint.");
            }
        }

        return new ErrorAndException("Communication avec le serveur MongoDB impossible.");
    }

    // Définition une exception personnalisée pour le serveur éteint
    public class ServerDownException extends RuntimeException {

        public ServerDownException(String message) {
            super(message);
        }
    }

    /* Exception liée au path pour l'url */
    @ResponseStatus(CONFLICT)
    @ExceptionHandler({ DuplicateKeyException.class })
    public @ResponseBody ErrorAndException duplicateKeyException(DuplicateKeyException exception) {
        return new ErrorAndException("tentative de violation d'unicité sur la clé");
    }

    /* Exception liée aux erreurs de syntax sur les requetes */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({ MongoCommandException.class })
    public @ResponseBody ErrorAndException mongoCommandException(MongoCommandException exception) {
        return new ErrorAndException("commande invalide");
    }

    /* Exception liée a l'echec de connexion a la bd */
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ MongoTimeoutException.class })
    public @ResponseBody ErrorAndException mongoTimeoutException(MongoTimeoutException exception) {
        return new ErrorAndException("le serveur s'est arreté en cours de traitement");
    }

    /* Exception liée a l'echec de connexion a la bd */
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ MongoNodeIsRecoveringException.class })
    public @ResponseBody ErrorAndException mongoNodeIsRecoveringException(MongoNodeIsRecoveringException exception) {
        return new ErrorAndException("le serveur n'est pas encore allumé");
    }

    /* Exception liée a l'echec de connexion a la bd */
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ MongoSocketOpenException.class })
    public @ResponseBody ErrorAndException mongoSocketOpenException(MongoSocketOpenException exception) {
        return new ErrorAndException("le serveur s'est arrete a linstant");
    }

    /* Exception liée a l'echec de connexion a la bd */
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ DataAccessResourceFailureException.class })
    public @ResponseBody ErrorAndException dataAccessResourceFailureException(
            DataAccessResourceFailureException exception) {
        return new ErrorAndException(" pas de donné le serveur s'est arrete a linstant");
    }

    /* Exception liée a l'echec de connexion a la bd */
    @ResponseStatus(SERVICE_UNAVAILABLE)
    @ExceptionHandler({ ConnectException.class })
    public @ResponseBody ErrorAndException connectException(ConnectException exception) {
        return new ErrorAndException(" pas de donné le serveur s'est arrete a linstant");
    }
     /* Exception liée a l'echec de connexion a la bd */
    //  @ResponseStatus(SMTPSendFailedException.class)
    //  @ExceptionHandler({ ConnectException.class })
    //  public @ResponseBody ErrorAndException connectException(ConnectException exception) {
    //      return new ErrorAndException(" pas de donné le serveur s'est arrete a linstant");
    //  }

}
