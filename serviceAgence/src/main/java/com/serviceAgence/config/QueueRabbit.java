package com.serviceAgence.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueRabbit {
    // queues agence vers Annonce
    public static final String QUEUE_Statut_compte = "Statut-Demande-Queue";
    public static final String QUEUE_Mt_passe = "Reset-PW-Queue";
    public static final String QUEUE_Statut_tranfert = "Statut-Transaction-Queue";
    public static final String QUEUE_Welcome = "Welcome-Queue";

    // queue agence vers carte
    public static final String QUEUE_Carte = "Transaction-Card-Queue";

    @Bean
    public Queue queue_1() {
        return new Queue(QUEUE_Statut_compte);
    }

    @Bean
    public Queue queue_2() {
        return new Queue(QUEUE_Statut_compte);
    }

    @Bean
    public Queue queue_3() {
        return new Queue(QUEUE_Statut_tranfert);
    }

    @Bean
    public Queue queue_4() {
        return new Queue(QUEUE_Welcome);
    }

    @Bean
    public Queue queue_5() {

        return new Queue(QUEUE_Carte);
    }

    /* configuration des queues de reception en cas d'absence */

    // reception d'une demande de creation de compte
    @Bean
    public Queue queue_r_demande() {
        return new Queue("Demande-Queue");
    }

    // reception de la reponse sur la demande par l'admin
    @Bean
    public Queue queue_r_reponse_demande() {
        return new Queue("Response-Queue");
    }

    // reception d'une demande de depot

    // reception d'une demande de transaction

    @Bean
    public Queue queue_r_demande_transaction() {
        return new Queue("Demande-Transaction-Queue");
    }
    // reception d'une demande de retrait

    @Bean
    public Queue queue_r_demande_retrait() {
        return new Queue("Demande-Retrait-Queue");
    }

    // reception d'une demande de mise a jour du mot de passe
    @Bean
    public Queue queue_r_demande_rreset() {
        return new Queue("Demande-Reset-passWord-Queue");

    }

}
