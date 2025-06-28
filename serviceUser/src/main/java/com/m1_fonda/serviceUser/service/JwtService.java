package com.m1_fonda.serviceUser.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 🔐 JWT Service
 * Handles JWT token generation, validation, and extraction for user authentication
 */
@Service
@Slf4j
public class JwtService {

    // JWT Configuration
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}") // 24 hours default
    private int jwtExpirationMs;
    
    @Value("${app.jwt.refresh-expiration:604800000}") // 7 days default
    private int refreshExpirationMs;

    private SecretKey getSigningKey() {
        // Ensure minimum 64 bytes for HS512
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            byte[] paddedKey = new byte[64];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
    /**
     * Generate JWT token for authenticated user
     */
    public String generateToken(String clientId, String email, String numero, String status) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("email", email);
        claims.put("numero", numero);
        claims.put("status", status);
        claims.put("role", "CLIENT");
        
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
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract email/subject from token
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract client ID from token
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
     * Extract role from token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
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
     * Extract all claims from token
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
        } catch (SignatureException e) {
            log.error("JWT signature invalid: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT token illegal argument: {}", e.getMessage());
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
     * Validate token without user details
     */
    public Boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Get remaining token validity time in milliseconds
     */
    public long getRemainingTokenTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if token needs refresh (expires in less than 2 hours)
     */
    public boolean needsRefresh(String token) {
        try {
            long remainingTime = getRemainingTokenTime(token);
            return remainingTime < (2 * 60 * 60 * 1000); // 2 hours in milliseconds
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Create token response object
     */
    public Map<String, Object> createTokenResponse(String token, String refreshToken, String clientId) {
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", token);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", jwtExpirationMs / 1000); // Convert to seconds
        response.put("clientId", clientId);
        response.put("issuedAt", new Date().getTime());
        
        return response;
    }

    /**
     * Extract user information from authentication context
     */
    public Map<String, Object> extractUserInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        
        if (authentication != null && authentication.isAuthenticated()) {
            userInfo.put("authenticated", true);
            userInfo.put("principal", authentication.getName());
            userInfo.put("authorities", authentication.getAuthorities());
        } else {
            userInfo.put("authenticated", false);
        }
        
        return userInfo;
    }
}