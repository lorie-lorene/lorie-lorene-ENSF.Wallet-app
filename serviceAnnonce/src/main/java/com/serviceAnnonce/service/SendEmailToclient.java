package com.serviceAnnonce.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SendEmailToclient {
    @Autowired
    private JavaMailSender mailSender;

    // Construire le corps de l'email en incluant tous les paramètres souhaités pour
    // la confirmation de la creation du compte
    public void sendEmail(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("moisechristianjunior464@gmail.com");
        message.setTo(email);
        message.setText("_________" + body + "_________\n" +
                "Cher utilisateur BIENVENUE, Votre compte a ete Creer avec un code personnalise, rendez-vous au pres de votre Agence  "
                + "\n" +
                "Nous vous prions de  garder votre code PIN et MOT DE PASSE de compte secrets!!  " + "\n"
                + "Merci...  ");
        message.setSubject("AGENCE DE" + " " + subject);
        mailSender.send(message);

    }

    // Construire le corps de l'email en incluant tous les paramètres souhaités pour
    // l'envoie du numero de compte de l'utlisateur
    public void sendEmail2(String email, String subject, long body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("moisechristianjunior464@gmail.com");
        message.setTo(email);
        message.setText("_________CONNEXION AU COMPTE_________\n" +
                "Cher utilisateur, Votre espace client est maintenant actif.\n" +
                "Votre code PIN compte est :[" + body + "]. Pour votre sécurité, ne le partagez avec personne.");
        message.setSubject("AGENCE DE" + " " + subject);
        mailSender.send(message);

    }

    public void sendEmailFail(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("moisechristianjunior464@gmail.com");
        message.setTo(email);
        message.setText("_________" + body + "_________\n" +
                "Cher utilisateur votre compte n'a pas pu etre cree  "
                + "\n" +
                "Nous vous prions de  reessayer en respectant les consignes !!  " + "\n"
                + "Merci...  ");
        message.setSubject("AGENCE DE" + " " + subject);
        mailSender.send(message);

    }

    /* fonction d'envoie des mails pour la mise a jour du mot de passe */
    public void sendEmailToUser(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("moisechristianjunior464@gmail.com");
        message.setTo(email);
        message.setText("___VOTRE MOT DE PASSE A ETE RENITIALISE___\n" +
                "Cher utilisateur, Votre espace client est redevenu actif, profitez de nos services\n" +
                "Votre mot de passe  est :[" + body + "]. Pour votre sécurité, ne le partagez avec personne.");
        message.setSubject("AGENCE DE" + " " + subject);
        mailSender.send(message);

    }

    /* fonction d'envoie des mails pour la mise a jour du mot de passe */
    public void sendEmailToUserForTransaction(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("moisechristianjunior464@gmail.com");
        message.setTo(email);
        message.setText("___OPERATION BANCAIRE EN COURS___\n" +
                "Cher utilisateur,suite a une tentative de transaction,\n" +
                "Votre compte a ete debite :[" + body + "]. Merci pour votre fidelite.");
        message.setSubject("" + " " + subject);
        mailSender.send(message);

    }
}
