package com.serviceAnnonce.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.serviceAgence.event.PasswordResetResponseEvent;
import com.serviceAnnonce.model.AnnonceEvent;
import com.serviceAnnonce.pojo.RegistrationResponseEvent;
import com.serviceAnnonce.pojo.TransactionNotificationEvent;
import com.serviceAnnonce.pojo.TransactionRequestEvent;
import com.serviceAnnonce.pojo.UpdateUser;

@Component
@Service
@Slf4j
@NoArgsConstructor
public class TransfertStatus {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SendEmailToclient sendEmailToclient;

    /*
     * reception de la reponse venant de agence et retransfere du statut vers
     * l'utilisateur
     */
    @RabbitListener(queues = "Statut-Demande-Queue")
    public void responseStatusToClient(RegistrationResponseEvent response) {
        AnnonceEvent annonce = new AnnonceEvent();

        annonce.setDescription(response.getProbleme());
        annonce.setTitre(response.getStatut());
        annonce.setId_agence(response.getIdAgence());
        annonce.setId_client(response.getIdClient());
        annonce.setEmail(response.getEmail());

        String email = response.getEmail();
        String agence = response.getIdAgence();
        String reponse = response.getProbleme();
        Long password = response.getNumeroCompte();
        System.out.print(email + " ");
        System.out.print(agence);

        switch (response.getStatut()) {

            case "REFUSE":

                rabbitTemplate.convertAndSend("Annonce-exchange", "request.response.annonce.to.client", annonce);
                sendEmailToclient.sendEmailFail(email, agence, reponse);

                break;
            case "ACCEPTE":

                sendEmailToclient.sendEmail(email, agence, reponse);
                sendEmailToclient.sendEmail2(email, agence, password);
                break;
            default:
                log.warn("Statut de la demande inconnu : {}", response.getProbleme());
        }
    }

    @RabbitListener(queues = "Reset-PW-Queue")
    public void newEmailToClient(PasswordResetResponseEvent user) {
        UpdateUser newUser = new UpdateUser();
        newUser.setNewPw(user.getNewPassword());
        newUser.setAgence(user.getAgence());
        newUser.setEmail(user.getEmail());
        newUser.setCni(user.getCni());

        if (user.getAgence() != null) {
            sendEmailToclient.sendEmailToUser(user.getEmail(), user.getAgence(), user.getNewPassword());

        } else {
            System.out.println("une eereur s'est produite");
        }

    }

    @RabbitListener(queues = "Statut-Transaction-Queue")
    public void notificationTransaction(TransactionNotificationEvent user) {
        TransactionRequestEvent event = new TransactionRequestEvent();
        event.setEventId(user.getEventId());
        event.setMontant(user.getMontant());
        event.setNumeroCompte(user.getCompteSource());
        event.setNumeroCompteDestination(user.getCompteDestination());
        event.setTimestamp(user.getTimestamp());
        String email = user.getEmail();
        String transaction = "OPERATION DE TRANSACTION";

        sendEmailToclient.sendEmailToUserForTransaction(email, transaction, user.getCompteDestination());

    }

}
