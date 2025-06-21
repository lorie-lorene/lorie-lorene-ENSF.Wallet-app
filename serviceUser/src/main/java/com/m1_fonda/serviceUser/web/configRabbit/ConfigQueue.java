package com.m1_fonda.serviceUser.web.configRabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigQueue {

    public static final String QUEUE_Entrant = "Demande-Queue";

    public static final String QUEUE_Entrant2 = "Demande-Depot-Queue";

    public static final String QUEUE_Entrant3 = "Demande-Transaction-Queue";

    public static final String QUEUE_Entrant4 = "Demande-Reset-passWord-Queue";

    public static final String QUEUE_Entrant5 = "Demande-Retrait-Queue";

    /* Client --> Agence */
    @Bean
    public Queue queue_1() {
        return new Queue(QUEUE_Entrant);
    }

    /* Client --> Agence */
    @Bean
    public Queue queue_2() {
        return new Queue(QUEUE_Entrant2);
    }

    /* Client --> Agence */
    @Bean
    public Queue queue_3() {
        return new Queue(QUEUE_Entrant3);
    }

    /* Client --> Agence */
    @Bean
    public Queue queue_4() {
        return new Queue(QUEUE_Entrant4);
    }

    /* Client --> Agence */
    @Bean
    public Queue queue_5() {
        return new Queue(QUEUE_Entrant5);
    }

    /* queues de reception en cas d'absence de celles ci */

    // reception du nouveau mot de passe
    @Bean
    public Queue queue_r_reset() {
        return new Queue("Response-Connexion-Client-Queue");
    }

    // suppresion du client en cas d'echec de creation du compte
    @Bean
    public Queue queue_r_request() {
        return new Queue("Response-Demande-Client-Queue");
    }
}
