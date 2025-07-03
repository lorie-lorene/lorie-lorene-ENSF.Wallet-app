package com.m1_fonda.serviceUser.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JwtService {

    // Injection des dépendances RabbitMQ pour la démonstration
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    // JWT Configuration
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}") // 24 hours default
    private int jwtExpirationMs;
    
    @Value("${app.jwt.refresh-expiration:604800000}") // 7 days default
    private int refreshExpirationMs;

    // Configuration RabbitMQ pour la validation des tokens (démonstration)
    @Value("${rabbitmq.exchange.auth:auth-exchange}")
    private String authExchange;
    
    @Value("${rabbitmq.routing.key.token.validation:token.validation.request}")
    private String tokenValidationRoutingKey;

    
    public boolean validateTokenViaRabbitMQ(String token, String requestedPath, String userId) {
        try {
            log.info("=== DÉMONSTRATION RABBITMQ TOKEN VALIDATION ===");
            log.info("Validation du token pour l'utilisateur {} tentant d'accéder à {}", userId, requestedPath);
            
            if (!isTokenValid(token)) {
                log.warn("Token invalide localement - Accès refusé immédiatement");
                return false;
            }
            
            Map<String, Object> validationRequest = createTokenValidationPayload(token, requestedPath, userId);
            
            boolean rabbitMQValidationResult = sendTokenValidationRequest(validationRequest);
            
            boolean agenceAuthorizationResult = simulateAgenceAuthorizationResponse(token, requestedPath);
            
            boolean finalDecision = rabbitMQValidationResult && agenceAuthorizationResult;
            
            log.info("=== RÉSULTAT DE LA VALIDATION RABBITMQ ===");
            log.info("Validation locale: OK");
            log.info("Validation RabbitMQ: {}", rabbitMQValidationResult ? "SUCCESS" : "FAILED");
            log.info("Autorisation Agence: {}", agenceAuthorizationResult ? "GRANTED" : "DENIED");
            log.info("Décision finale: {}", finalDecision ? "ACCÈS AUTORISÉ" : "ACCÈS REFUSÉ");
            
            return finalDecision;
            
        } catch (Exception e) {
            log.error("Erreur lors de la validation RabbitMQ du token: {}", e.getMessage(), e);
            return false;
        }
    }
   
    private Map<String, Object> createTokenValidationPayload(String token, String requestedPath, String userId) {
        Map<String, Object> payload = new HashMap<>();
        
        try {
            // Informations de base
            payload.put("userId", userId);
            payload.put("token", token);
            payload.put("requestedPath", requestedPath);
            payload.put("timestamp", new Date().toString());
            payload.put("requestId", UUID.randomUUID().toString());
            
            // Extraction des claims du token pour validation
            String clientId = extractClientId(token);
            String role = extractRole(token);
            String service = extractService(token);
            
            payload.put("clientId", clientId);
            payload.put("role", role);
            payload.put("service", service != null ? service : "UserService");
            
            // Informations de sécurité
            payload.put("action", "PATH_ACCESS_VALIDATION");
            payload.put("source", "UserService");
            payload.put("validationType", "RBAC_CHECK"); // Role-Based Access Control
            
            log.debug("Payload de validation créé pour l'utilisateur {} - Chemin: {}", userId, requestedPath);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du payload de validation: {}", e.getMessage());
            payload.put("error", "PAYLOAD_CREATION_FAILED");
        }
        
        return payload;
    }
    
   
    private boolean sendTokenValidationRequest(Map<String, Object> validationRequest) {
        try {
            log.info("Envoi de la requête de validation vers le service Agence via RabbitMQ...");
            
            // Conversion en JSON pour l'envoi
            String jsonPayload = objectMapper.writeValueAsString(validationRequest);
            
            // Envoi via RabbitMQ (démonstration)
            rabbitTemplate.convertAndSend(
                authExchange,
                tokenValidationRoutingKey,
                jsonPayload
            );
            
            log.info("Requête de validation envoyée avec succès via RabbitMQ");
            log.debug("Exchange: {} | Routing Key: {}", authExchange, tokenValidationRoutingKey);
            
            // Simulation d'un délai de traitement
            simulateProcessingDelay();
            
            return true;
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la requête RabbitMQ: {}", e.getMessage());
            return false;
        }
    }
    
   
    private boolean simulateAgenceAuthorizationResponse(String token, String requestedPath) {
        try {
            log.info("Simulation de la réponse du service Agence...");
            
            // Simulation de règles d'autorisation basées sur le rôle
            String role = extractRole(token);
            String service = extractService(token);
            
            // Règles de démonstration
            if ("ADMIN".equals(role) || "SUPERVISOR".equals(role)) {
                log.info("Utilisateur avec rôle administrateur - Accès autorisé à tous les chemins");
                return true;
            }
            
            if ("CLIENT".equals(role)) {
                // Les clients ne peuvent accéder qu'à certains chemins
                if (requestedPath.startsWith("/client/") || 
                    requestedPath.startsWith("/public/") ||
                    requestedPath.equals("/dashboard")) {
                    log.info("Client autorisé à accéder au chemin: {}", requestedPath);
                    return true;
                } else {
                    log.warn("Client non autorisé à accéder au chemin: {}", requestedPath);
                    return false;
                }
            }
            
            if ("AgenceService".equals(service)) {
                log.info("Token du service Agence - Accès privilégié autorisé");
                return true;
            }
            
            log.warn("Aucune règle d'autorisation trouvée pour le rôle: {} sur le chemin: {}", role, requestedPath);
            return false;
            
        } catch (Exception e) {
            log.error("Erreur lors de la simulation de l'autorisation Agence: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Simule un délai de traitement pour rendre la démonstration plus réaliste
     */
    private void simulateProcessingDelay() {
        try {
            // Simulation d'un délai de 100ms pour le traitement RabbitMQ
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Délai de simulation interrompu");
        }
    }
    
    /**
     * Méthode utilitaire pour logger les détails du token (démonstration)
     * 
     * @param token Token à analyser
     */
    public void logTokenDetails(String token) {
        try {
            log.info("=== DÉTAILS DU TOKEN (DÉMONSTRATION) ===");
            log.info("Client ID: {}", extractClientId(token));
            log.info("Role: {}", extractRole(token));
            log.info("Service: {}", extractService(token));
            log.info("Subject: {}", extractSubject(token));
            log.info("Expiration: {}", extractExpiration(token));
            log.info("Token valide: {}", isTokenValid(token));
            log.info("Type de service: {}", isAgenceServiceToken(token) ? "AgenceService" : "UserService");
            log.info("Privilèges admin: {}", hasAdminRole(token));
            log.info("=====================================");
        } catch (Exception e) {
            log.error("Erreur lors de l'affichage des détails du token: {}", e.getMessage());
        }
    }

   
    /**
     * Enhanced signing key generation to ensure HS512 compatibility
     */
    private SecretKey getSigningKey() {
        // Ensure minimum 64 bytes for HS512
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            log.warn("JWT secret too short ({}), padding to 64 bytes for HS512", keyBytes.length);
            byte[] paddedKey = new byte[64];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT token for authenticated user (UserService style)
     */
    public String generateToken(String clientId, String email, String numero, String status) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("email", email);
        claims.put("numero", numero);
        claims.put("status", status);
        claims.put("role", "CLIENT");
        claims.put("service", "UserService"); // Add service identifier
        
        return createToken(claims, email);
    }

    /**
     * Generate JWT token with custom claims
     */
    public String generateToken(Map<String, Object> extraClaims, String subject) {
        return createToken(extraClaims, subject);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512) // Use HS512 for consistency
                .compact();
    }

    /**
     * Create JWT token with claims - Updated for HS512
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512) // Use HS512 consistently
                .compact();
    }

    /**
     * Extract email/subject from token
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract client ID from token (UserService specific)
     */
    public String extractClientId(String token) {
        return extractClaim(token, claims -> claims.get("clientId", String.class));
    }

    /**
     * Extract user status from token
     */
    public String extractStatus(String token) {
        return extractClaim(token, claims -> claims.get("status", String.class));
    }

    /**
     * Extract phone number from token
     */
    public String extractNumero(String token) {
        return extractClaim(token, claims -> claims.get("numero", String.class));
    }

    /**
     * Extract role from token (supports both UserService and AgenceService formats)
     */
    public String extractRole(String token) {
        // Try UserService format first
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        if (role != null) {
            return role;
        }
        
        // Try AgenceService format (roles as comma-separated string)
        String roles = extractClaim(token, claims -> claims.get("roles", String.class));
        if (roles != null && !roles.isEmpty()) {
            // Return first role for compatibility
            return roles.split(",")[0].trim();
        }
        
        return null;
    }

    /**
     * Extract service identifier from token
     */
    public String extractService(String token) {
        return extractClaim(token, claims -> claims.get("service", String.class));
    }

    /**
     * Extract authorities from token (supports both token types)
     */
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String service = claims.get("service", String.class);
            
            if ("AgenceService".equals(service)) {
                // Handle AgenceService tokens (admin roles)
                String roles = claims.get("roles", String.class);
                if (roles != null) {
                    return Arrays.stream(roles.split(","))
                            .map(role -> new SimpleGrantedAuthority(role.trim()))
                            .collect(Collectors.toList());
                }
            } else {
                // Handle UserService tokens (client role)
                String role = claims.get("role", String.class);
                if (role != null) {
                    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error extracting authorities from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token with enhanced error handling
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token malformed: {}", e.getMessage());
            throw e;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("JWT signature invalid: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT token illegal argument: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT token validation error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token against user details
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractSubject(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Enhanced token validation without user details (supports both token types)
     */
    public Boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean isNotExpired = !isTokenExpired(token);
            
            if (!isNotExpired) {
                log.warn("Token is expired");
                return false;
            }
            
            // Check token type and validate accordingly
            String service = claims.get("service", String.class);
            
            if ("AgenceService".equals(service)) {
                // Validate AgenceService token
                String roles = claims.get("roles", String.class);
                String subject = claims.getSubject();
                
                boolean isValidAgenceToken = roles != null && subject != null;
                if (isValidAgenceToken) {
                    log.debug("Valid AgenceService admin token for user: {}", subject);
                }
                return isValidAgenceToken;
                
            } else {
                // Validate UserService token (default)
                String role = claims.get("role", String.class);
                String clientId = claims.get("clientId", String.class);
                
                boolean isValidUserToken = role != null && clientId != null;
                if (isValidUserToken) {
                    log.debug("Valid UserService client token for client: {}", clientId);
                }
                return isValidUserToken;
            }
            
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Check if user has admin privileges (from AgenceService token)
     */
    public boolean hasAdminRole(String token) {
        try {
            String service = extractService(token);
            if ("AgenceService".equals(service)) {
                String roles = extractClaim(token, claims -> claims.get("roles", String.class));
                return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPERVISOR"));
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking admin role: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is from AgenceService
     */
    public boolean isAgenceServiceToken(String token) {
        try {
            String service = extractService(token);
            return "AgenceService".equals(service);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is from UserService
     */
    public boolean isUserServiceToken(String token) {
        try {
            String service = extractService(token);
            return service == null || "UserService".equals(service); // null means legacy UserService token
        } catch (Exception e) {
            return false;
        }
    }
}