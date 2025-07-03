package com.Authentication.authService.service;

 

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration

public class RabbitConfig {
    
    @Value("${rabbitmq.exchange.auth}")
    private String authExchange;
    
    @Value("${rabbitmq.queue.agence.security}")
    private String agenceSecurityQueue;
    
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(authExchange);
    }
    
    @Bean
    public org.springframework.amqp.core.Queue agenceSecurityQueue() {
        return QueueBuilder.durable(agenceSecurityQueue).build();
    }
    
    @Bean
    public org.springframework.amqp.core.Binding tokenDistributionBinding() {
        return BindingBuilder
            .bind(agenceSecurityQueue())
            .to(authExchange())
            .with("token.#");
    }
}