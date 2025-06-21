package com.serviceAnnonce.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueRabbit {

    public static final String QUEUE_Statut = "Statut-Demande-Queue";
    public static final String QUEUE_Client = "Response-Demande-Client-Queue";
    public static final String QUEUE_PW = "Reset-PW-Queue";
    public static final String QUEUE_Transaction = "Statut-Transaction-Queue";

    @Bean
    public Queue queue_1() {
        return new Queue(QUEUE_Statut);
    }

    @Bean
    public Queue queue_2() {
        return new Queue(QUEUE_Client);
    }

    @Bean
    public Queue queue_3() {
        return new Queue(QUEUE_PW);
    }

    @Bean
    public Queue queue_4() {
        return new Queue(QUEUE_Transaction);
    }

}
