package com.serviceAgence.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class ConfigRabbit {
    public static final String EXCHANGE = "Agence-exchange";
    public static final String KEY = "agence.registration.response";
    public static final String KEY2 = "agence.password.reset.response";
    public static final String KEY3 = "agence.transaction.response";
    public static final String KEY4 = "welcome.client";
    public static final String KEY5 = "agence.transaction.card.response";

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

    // ==========================================
    // FIXED BINDINGS WITH @Qualifier
    // ==========================================

    @Bean
    public Binding binding1(@Qualifier("queue_1") Queue queue, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue).to(topicExchange).with(KEY);
    }

    @Bean
    public Binding binding2(@Qualifier("queue_2") Queue queue, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue).to(topicExchange).with(KEY2);
    }

    @Bean
    public Binding binding3(@Qualifier("queue_3") Queue queue, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue).to(topicExchange).with(KEY3);
    }

    @Bean
    public Binding binding4(@Qualifier("queue_4") Queue queue, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue).to(topicExchange).with(KEY4);
    }

    @Bean
    public Binding binding5(@Qualifier("queue_5") Queue queue, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue).to(topicExchange).with(KEY5);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Méthode utilitaire pour créer dynamiquement une queue si elle n'existe pas
    public static Queue createQueueIfNotExists(String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    // Méthode utilitaire pour créer un binding dynamique
    public static Binding createBinding(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}