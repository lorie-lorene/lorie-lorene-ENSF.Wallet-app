package com.serviceDemande.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    
    // Exchanges
    public static final String DEMANDE_EXCHANGE = "Demande-exchange";
    
    // Queues - Entrantes (reçoit d'Agence)
    public static final String VALIDATION_REQUEST_QUEUE = "Validation-Demande-Queue";
    public static final String TRANSACTION_VALIDATION_QUEUE = "Transaction-Validation-Queue";
    
    // Queues - Sortantes (envoie vers Agence)
    public static final String VALIDATION_RESPONSE_QUEUE = "Validation-Response-Queue";
    public static final String LIMITS_UPDATE_QUEUE = "Limits-Update-Queue";
    public static final String FRAUD_ALERT_QUEUE = "Fraud-Alert-Queue";
    
    // Routing Keys
    public static final String VALIDATION_RESPONSE_KEY = "demande.validation.response";
    public static final String LIMITS_UPDATE_KEY = "demande.limits.update";
    public static final String FRAUD_ALERT_KEY = "demande.fraud.alert";
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    
    @Bean
    public TopicExchange demandeExchange() {
        return new TopicExchange(DEMANDE_EXCHANGE);
    }
    
    // Queues
    @Bean
    public Queue validationRequestQueue() {
        return QueueBuilder.durable(VALIDATION_REQUEST_QUEUE).build();
    }
    
    @Bean
    public Queue transactionValidationQueue() {
        return QueueBuilder.durable(TRANSACTION_VALIDATION_QUEUE).build();
    }
    
    @Bean
    public Queue validationResponseQueue() {
        return QueueBuilder.durable(VALIDATION_RESPONSE_QUEUE).build();
    }
    
    @Bean
    public Queue limitsUpdateQueue() {
        return QueueBuilder.durable(LIMITS_UPDATE_QUEUE).build();
    }
    
    @Bean
    public Queue fraudAlertQueue() {
        return QueueBuilder.durable(FRAUD_ALERT_QUEUE).build();
    }
    
    // Bindings
    @Bean
    public Binding validationResponseBinding() {
        return BindingBuilder.bind(validationResponseQueue())
                .to(demandeExchange())
                .with(VALIDATION_RESPONSE_KEY);
    }
    
    @Bean
    public Binding limitsUpdateBinding() {
        return BindingBuilder.bind(limitsUpdateQueue())
                .to(demandeExchange())
                .with(LIMITS_UPDATE_KEY);
    }
    
    @Bean
    public Binding fraudAlertBinding() {
        return BindingBuilder.bind(fraudAlertQueue())
                .to(demandeExchange())
                .with(FRAUD_ALERT_KEY);
    }
    
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}