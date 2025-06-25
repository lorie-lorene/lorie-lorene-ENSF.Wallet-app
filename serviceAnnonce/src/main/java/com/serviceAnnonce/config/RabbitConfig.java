package com.serviceAnnonce.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "Annonce-exchange";
    /*
     * cle pour envoyer les reponses a un utlisateur par rapport a son inscription
     */
    public static final String kEY = "request.response.annonce.to.client";

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public TopicExchange topicExchange() {

        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue_2, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_2).to(EXCHANGE).with(kEY);
    }

// serviceAnnonce/src/main/java/com/serviceAnnonce/config/RabbitConfig.java
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        // Configure ObjectMapper for better compatibility
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        objectMapper.registerModule(new JavaTimeModule());

        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
