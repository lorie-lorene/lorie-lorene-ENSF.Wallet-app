package com.m1_fonda.serviceUser;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.mockito.Mockito;

/**
 * Configuration de test pour remplacer certains beans en mode test
 */
@Configuration
public class TestConfiguration {

    /**
     * PasswordEncoder pour les tests avec force réduite pour performance
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4); // Force réduite pour tests rapides
    }

    /**
     * RabbitTemplate mocké pour éviter les connexions réelles
     */
    @Bean
    @Primary
    public RabbitTemplate mockRabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    /**
     * ConnectionFactory mocké
     */
    @Bean
    @Primary
    public ConnectionFactory mockConnectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }

    /**
     * Message converter pour les tests
     */
    @Bean
    public Jackson2JsonMessageConverter testMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}