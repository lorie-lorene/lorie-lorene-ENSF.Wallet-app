package com.serviceAgence.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * Utilitaire pour la génération et validation des tokens JWT
 * Compatible avec les tokens générés par UserService
 * Version mise à jour pour compatibilité JJWT 0.12+
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;
    
    /**
     * Constructeur avec injection des propriétés JWT
     * Gère l'initialisation sécurisée de la clé secrète
     */
    public JwtTokenProvider(
        @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidation123456789}") String jwtSecret,
        @Value("${jwt.expiration-ms:86400000}") long jwtExpirationMs) {

        try {
            // Tentative de décodage Base64, sinon utilisation directe du string
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(jwtSecret);
                log.debug("Clé JWT décodée depuis Base64");
            } catch (IllegalArgumentException e) {
                // Si ce n'est pas du Base64 valide, utiliser les bytes du string directement
                keyBytes = jwtSecret.getBytes();
                log.debug("Clé JWT utilisée depuis string direct");
            }
            
            // Assurer que la clé fait au moins 512 bits (64 bytes) pour HS512
            if (keyBytes.length < 64) {
                byte[] paddedKey = new byte[64];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
                keyBytes = paddedKey;
                log.debug("Clé JWT étendue à 64 bytes pour HS512");
            }
            
            this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
            this.jwtExpirationMs = jwtExpirationMs;
            
            log.info("🔐 JWT Provider initialisé - Expiration: {}ms", jwtExpirationMs);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du JWT Provider: {}", e.getMessage());
            throw new RuntimeException("Impossible d'initialiser le JWT Provider", e);
        }
    }

    /**
     * Génération token JWT pour authentification admin/agence
     * Utilise la nouvelle API JJWT 0.12+
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(username)                              // Nouvelle API: subject() au lieu de setSubject()
                .claim("roles", roles)
                .claim("service", "AgenceService")
                .issuedAt(Date.from(now))                      // Nouvelle API: issuedAt() au lieu de setIssuedAt()
                .expiration(Date.from(expiryDate))             // Nouvelle API: expiration() au lieu de setExpiration()
                .signWith(jwtSecretKey, Jwts.SIG.HS512)        // Nouvelle API: Jwts.SIG.HS512 au lieu de SignatureAlgorithm.HS512
                .compact();
    }

    /**
     * Extraction username depuis token
     * Utilise la nouvelle API de parsing JJWT 0.12+
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()                      // Nouvelle API: parser() au lieu de parserBuilder()
                    .verifyWith(jwtSecretKey)                  // Nouvelle API: verifyWith() au lieu de setSigningKey()
                    .build()
                    .parseSignedClaims(token)                  // Nouvelle API: parseSignedClaims() au lieu de parseClaimsJws()
                    .getPayload();                             // Nouvelle API: getPayload() au lieu de getBody()
            
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Erreur extraction username: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extraction rôles depuis token
     * Gestion robuste des erreurs de parsing
     */
    public String getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.get("roles", String.class);
        } catch (Exception e) {
            log.error("Erreur extraction rôles: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validation token JWT
     * Gestion complète des différents types d'exceptions
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Signature JWT invalide: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformé: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token JWT expiré: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT non supporté: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Token JWT vide: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erreur validation token JWT: {}", e.getMessage());
        }
        
        return false;
    }

    /**
     * Obtention date expiration
     * Extraction sécurisée des claims
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Erreur extraction date expiration: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Vérification si token expiré
     * Gestion des cas d'erreur et null safety
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.error("Erreur vérification expiration token: {}", e.getMessage());
            return true; // Considérer comme expiré en cas d'erreur
        }
    }

    /**
     * Extraction de tous les claims du token
     * Méthode utilitaire pour debugging ou usage avancé
     */
    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Erreur extraction claims: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Vérification de la validité du token avec informations détaillées
     * Retourne les détails de validation pour debugging
     */
    public TokenValidationResult validateTokenWithDetails(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            boolean isExpired = claims.getExpiration().before(new Date());
            
            return TokenValidationResult.builder()
                    .valid(!isExpired)
                    .expired(isExpired)
                    .username(claims.getSubject())
                    .roles(claims.get("roles", String.class))
                    .expirationDate(claims.getExpiration())
                    .build();
                    
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.builder()
                    .valid(false)
                    .expired(true)
                    .errorMessage("Token expiré")
                    .build();
        } catch (Exception e) {
            return TokenValidationResult.builder()
                    .valid(false)
                    .expired(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Classe pour encapsuler les résultats de validation
     */
    @lombok.Builder
    @lombok.Data
    public static class TokenValidationResult {
        private boolean valid;
        private boolean expired;
        private String username;
        private String roles;
        private Date expirationDate;
        private String errorMessage;
    }
}