package com.serviceAgence.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class ConfigRabbit {
    public static final String EXCHANGE = "Agence-exchange";
    public static final String KEY = "request.send";
    public static final String KEY2 = "request.response.to.client";
    public static final String KEY3 = "depot.response.to.client";
    public static final String KEY4 = "transaction.response.to.client";
    public static final String KEY5 = "connexion.response.to.client";
    public static final String KEY6 = "retrait.message";
    public static final String KEY7 = "retrait.response.to.client";

    // Noms des queues
    public static final String QUEUE_1 = "agence.request.queue";
    public static final String QUEUE_2 = "agence.response.client.queue";
    public static final String QUEUE_3 = "depot.response.client.queue";
    public static final String QUEUE_4 = "transaction.response.client.queue";
    public static final String QUEUE_5 = "connexion.response.client.queue";
    public static final String QUEUE_6 = "retrait.message.queue";
    public static final String QUEUE_7 = "retrait.response.client.queue";

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

    // Déclaration des queues avec création automatique
    @Bean
    public Queue queue1() {
        return QueueBuilder.durable(QUEUE_1).build();
    }

    @Bean
    public Queue queue2() {
        return QueueBuilder.durable(QUEUE_2).build();
    }

    @Bean
    public Queue queue3() {
        return QueueBuilder.durable(QUEUE_3).build();
    }

    @Bean
    public Queue queue4() {
        return QueueBuilder.durable(QUEUE_4).build();
    }

    @Bean
    public Queue queue5() {
        return QueueBuilder.durable(QUEUE_5).build();
    }

    @Bean
    public Queue queue6() {
        return QueueBuilder.durable(QUEUE_6).build();
    }

    @Bean
    public Queue queue7() {
        return QueueBuilder.durable(QUEUE_7).build();
    }

    // Bindings - utilisation des beans de queues
    @Bean
    public Binding binding1(Queue queue1, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue1).to(topicExchange).with(KEY);
    }

    @Bean
    public Binding binding2(Queue queue2, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue2).to(topicExchange).with(KEY2);
    }

    @Bean
    public Binding binding3(Queue queue3, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue3).to(topicExchange).with(KEY3);
    }

    @Bean
    public Binding binding4(Queue queue4, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue4).to(topicExchange).with(KEY4);
    }

    @Bean
    public Binding binding5(Queue queue5, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue5).to(topicExchange).with(KEY5);
    }

    @Bean
    public Binding binding6(Queue queue6, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue6).to(topicExchange).with(KEY6);
    }

    @Bean
    public Binding binding7(Queue queue7, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue7).to(topicExchange).with(KEY7);
    }

    // Alternative: Déclaration groupée avec Declarables
    @Bean
    public Declarables topicBindings() {
        return new Declarables(
                // Queues
                queue1(), queue2(), queue3(), queue4(), queue5(), queue6(), queue7(),
                // Exchange
                topicExchange(),
                // Bindings
                BindingBuilder.bind(queue1()).to(topicExchange()).with(KEY),
                BindingBuilder.bind(queue2()).to(topicExchange()).with(KEY2),
                BindingBuilder.bind(queue3()).to(topicExchange()).with(KEY3),
                BindingBuilder.bind(queue4()).to(topicExchange()).with(KEY4),
                BindingBuilder.bind(queue5()).to(topicExchange()).with(KEY5),
                BindingBuilder.bind(queue6()).to(topicExchange()).with(KEY6),
                BindingBuilder.bind(queue7()).to(topicExchange()).with(KEY7));
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

    /*
     * UTILISATION:
     * 
     * Pour envoyer un message:
     * rabbitTemplate.convertAndSend(EXCHANGE, KEY, message);
     * 
     * Pour écouter:
     * 
     * @RabbitListener(queues = QUEUE_1)
     * public void handleMessage(Message message) { ... }
     * 
     * Les queues seront créées automatiquement au démarrage de l'application
     * grâce aux beans @Bean déclarés ci-dessus.
     */
}