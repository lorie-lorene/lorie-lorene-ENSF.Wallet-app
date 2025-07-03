package com.Authentication.authService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
public class AuthServiceRabbit {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${rabbitmq.exchange.auth}")
    private String authExchange;
    
    @Value("${rabbitmq.routing.key.token.distribution}")
    private String tokenDistributionRoutingKey;
    
    @Value("${rabbitmq.queue.agence.security}")
    private String agenceSecurityQueue;
    
    public boolean sendTokenToAgenceService(Long userId, String token, Long agenceId) {
        try {
            logger.info("Début de l'envoi du token pour l'utilisateur {} vers l'agence {}", userId, agenceId);
            
            Map<String, Object> securityPayload = createSecurityPayload(userId, token, agenceId);
            
            String jsonPayload = objectMapper.writeValueAsString(securityPayload);
            
            rabbitTemplate.convertAndSend(
                authExchange, 
                tokenDistributionRoutingKey, 
                jsonPayload
            );
            
            logger.info("Token envoyé avec succès au service Agence via RabbitMQ - Queue: {}", agenceSecurityQueue);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi du token au service Agence: {}", e.getMessage(), e);
            return false;
        }
    }
   
    private Map<String, Object> createSecurityPayload(Long userId, String token, Long agenceId) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("userId", userId);
        payload.put("agenceId", agenceId);
        payload.put("token", token);
        
        // Métadonnées de sécurité
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("action", "TOKEN_DISTRIBUTION");
        payload.put("source", "AUTH_SERVICE");
        payload.put("destination", "AGENCE_SERVICE");
        
        payload.put("expiresIn", 3600); 
        payload.put("tokenType", "Bearer");
        
        logger.debug("Payload de sécurité créé pour l'utilisateur {} et l'agence {}", userId, agenceId);
        
        return payload;
    }
    
   
    public void revokeTokenFromAgenceService(Long userId, String tokenId, Long agenceId) {
        try {
            logger.info("Révocation du token {} pour l'utilisateur {} de l'agence {}", tokenId, userId, agenceId);
            
            // Création du payload de révocation
            Map<String, Object> revocationPayload = new HashMap<>();
            revocationPayload.put("userId", userId);
            revocationPayload.put("agenceId", agenceId);
            revocationPayload.put("tokenId", tokenId);
            revocationPayload.put("action", "TOKEN_REVOCATION");
            revocationPayload.put("timestamp", LocalDateTime.now().toString());
            revocationPayload.put("source", "AUTH_SERVICE");
            
            // Envoi via RabbitMQ
            String jsonPayload = objectMapper.writeValueAsString(revocationPayload);
            rabbitTemplate.convertAndSend(
                authExchange, 
                "token.revocation", 
                jsonPayload
            );
            
            logger.info("Notification de révocation envoyée avec succès au service Agence");
            
        } catch (Exception e) {
            logger.error("Erreur lors de la révocation du token: {}", e.getMessage(), e);
        }
    }
    
    public boolean checkRabbitMQConnection() {
        try {
            logger.info("Vérification de la connexion RabbitMQ...");
            
            logger.info("Connexion RabbitMQ active - Prêt pour l'envoi de tokens");
            return true;
            
        } catch (Exception e) {
            logger.error("Problème de connexion RabbitMQ: {}", e.getMessage());
            return false;
        }
    }
}