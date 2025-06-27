package com.m1_fonda.serviceUser.web.configRabbit;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class ConfigRabbit {
    public static final String EXCHANGE = "Client-exchange";
    /* cle pour l'envoie d'une demande */
    public static final String kEY = "demande.send";
    public static final String kEY6 = "demande.send2";
    /* cle pour l'envoie d'un depot */
    public static final String kEY2 = "depot.send";
    /* cle pour lenvoie d'un retrait */
    public static final String kEY5 = "retrait.send";
    /* cle pour l'envoie d'une transaction */
    public static final String kEY3 = "transaction.send";
    /* cle pour l'envoie d'une demande de reconnexion */
    public static final String kEY4 = "connexion.send";

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
    public Binding binding(Queue queue_1, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_1).to(EXCHANGE).with(kEY);
    }

    @Bean
    public Binding binding2(Queue queue_2, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_2).to(EXCHANGE).with(kEY2);
    }

    @Bean
    public Binding binding3(Queue queue_3, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_3).to(EXCHANGE).with(kEY3);
    }

    @Bean
    public Binding binding4(Queue queue_4, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_4).to(EXCHANGE).with(kEY4);
    }

    @Bean
    public Binding binding5(Queue queue_5, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_5).to(EXCHANGE).with(kEY5);
    }

    @Bean
    public Binding binding6(Queue queue_6, TopicExchange EXCHANGE) {
        return BindingBuilder.bind(queue_6).to(EXCHANGE).with(kEY6);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
