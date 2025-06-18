package com.serviceAgence.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueRabbit {

    public static final String QUEUE_Entrant = "Validation-Demande-Queue";
    /* Agence --> Demande */
    public static final String QUEUE_Sortant = "Response-Demande-Queue";
    /* Agence --> Annonce */
    public static final String QUEUE_Sortant2 = "Demande-Depot-confirm-Queue";

    public static final String QUEUE_Sortant6 = "Demande-Retrait-confirm-Queue";
    /* Agence --> Depot */
    public static final String QUEUE_Sortant3 = "Demande-Transaction-confirm-Queue";
    /* Agence --> User */
    public static final String QUEUE_Sortant4 = "Demande-Connexion-Queue";
    /* Agence --> Retrait */
    public static final String QUEUE_Sortant5 = "Retrait-Agence-Queue";

    @Bean
    public Queue queue_1() {
        return new Queue(QUEUE_Entrant);
    }

    @Bean
    public Queue queue_2() {
        return new Queue(QUEUE_Sortant);
    }

    @Bean
    public Queue queue_3() {
        return new Queue(QUEUE_Sortant2);
    }

    @Bean
    public Queue queue_4() {
        return new Queue(QUEUE_Sortant3);
    }

    @Bean
    public Queue queue_5() {
        return new Queue(QUEUE_Sortant4);
    }

    @Bean
    public Queue queue_6() {
        return new Queue(QUEUE_Sortant5);
    }

    @Bean
    public Queue queue_7() {

        return new Queue(QUEUE_Sortant6);
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
    @Bean
    public Queue queue_r_demande_depot() {
        return new Queue("Validation-Demande-Depot-Queue");
    }
    // reception d'une demande de transaction

    @Bean
    public Queue queue_r_demande_transaction() {
        return new Queue("Validation-Demande-Transaction-Queue");
    }
    // reception d'une demande de retrait

    @Bean
    public Queue queue_r_demande_retrait() {
        return new Queue("Retrait-Agence-Queue");
    }

    // reception d'une demande de mise a jour du mot de passe
    @Bean
    public Queue queue_r_demande_rreset() {
        return new Queue("Demande-Reset-passWord-Queue");

    }

    @Bean
    public Queue queue_r_demande_retrait_r() {
        return new Queue("Validation-Demande-Retrait-Queue");
    }
}
