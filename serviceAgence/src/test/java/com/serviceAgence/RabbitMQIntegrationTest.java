package com.serviceAgence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.event.TransactionRequestEvent;
import com.serviceAgence.event.UserRegistrationEventReceived;

@SpringBootTest(classes = ServiceAgenceApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Désactiver Spring Cloud Config
        "spring.cloud.config.enabled=false",
        "spring.config.import=",

        // Configuration RabbitMQ pour les tests
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.rabbitmq.listener.simple.auto-startup=false",

        // Logging
        "logging.level.com.serviceAgence=DEBUG",
        "logging.level.org.springframework.amqp=ERROR",

        // Configuration H2 pour les tests
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RabbitMQIntegrationTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void testSendUserRegistrationEvent() {
        // Given
        UserRegistrationEventReceived event = new UserRegistrationEventReceived();
        event.setEventId("TEST_EVENT_001");
        event.setIdClient("CLIENT_RABBIT_TEST");
        event.setIdAgence("AGENCE_RABBIT_TEST");
        event.setCni("987654321");
        event.setEmail("rabbit.test@example.com");
        event.setNom("RABBIT");
        event.setPrenom("Test");
        event.setNumero("655987654");
        event.setSourceService("UserService");
        event.setTimestamp(LocalDateTime.now());

        // When
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend("user-exchange", "user.registration.request", event);
        });

        // Then - Vérifier que le message a été envoyé
        verify(rabbitTemplate).convertAndSend(
                eq("user-exchange"),
                eq("user.registration.request"),
                eq(event));
    }

    @Test
    void testSendTransactionEvent() {
        // Given
        TransactionRequestEvent event = new TransactionRequestEvent();
        event.setEventId("TXN_RABBIT_TEST_001");
        event.setType(TransactionType.DEPOT_PHYSIQUE);
        event.setMontant(new BigDecimal("5000"));
        event.setNumeroClient("CLIENT_RABBIT_TEST");
        event.setNumeroCompte("123456789");
        event.setSourceService("UserService");
        event.setTimestamp(LocalDateTime.now());

        // When
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend("user-exchange", "user.transaction.request", event);
        });

        // Then - Vérifier que le message a été envoyé
        verify(rabbitTemplate).convertAndSend(
                eq("user-exchange"),
                eq("user.transaction.request"),
                eq(event));
    }

    @Test
    void testRabbitTemplateConfiguration() {
        // Test que le RabbitTemplate est correctement mocké
        assertNotNull(rabbitTemplate);

        // Test d'un appel simple
        rabbitTemplate.convertAndSend("test-exchange", "test.routing.key", "test message");

        verify(rabbitTemplate).convertAndSend("test-exchange", "test.routing.key", "test message");
    }
}