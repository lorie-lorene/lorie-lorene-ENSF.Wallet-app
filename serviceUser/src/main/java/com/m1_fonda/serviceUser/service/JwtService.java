package com.m1_fonda.serviceUser.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 🔐 Enhanced JWT Service
 * Handles JWT token generation, validation, and extraction for user authentication
 * Now supports both UserService client tokens and AgenceService admin tokens
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