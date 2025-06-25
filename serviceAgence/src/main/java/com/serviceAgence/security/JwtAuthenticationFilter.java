package com.serviceAgence.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtre d'authentification JWT pour les requêtes entrantes
 * Valide le token et configure le contexte de sécurité
 * Compatible CORS
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Bypass complet pour les requêtes OPTIONS (CORS preflight)
        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extraction du token depuis l'en-tête Authorization
            String jwt = getTokenFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Token valide - configuration du contexte de sécurité
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String rolesString = jwtTokenProvider.getRolesFromToken(jwt);
                
                if (username != null && rolesString != null) {
                    // Conversion des rôles en authorities
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                            .map(role -> new SimpleGrantedAuthority(role.trim()))
                            .collect(Collectors.toList());
                    
                    // Création du token d'authentification
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Configuration du contexte de sécurité
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("🔐 Utilisateur authentifié: {} avec rôles: {}", username, rolesString);
                }
            } else {
                log.debug("🔓 Requête sans token JWT valide: {} {}", request.getMethod(), request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("❌ Erreur configuration authentification: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        // Continuation du filtrage
        filterChain.doFilter(request, response);
    }

    /**
     * Extraction du token JWT depuis l'en-tête Authorization
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Suppression du préfixe "Bearer "
        }
        
        return null;
    }

    /**
     * Ignore les endpoints publics - SIMPLIFIÉ pour éviter les conflits CORS
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // TOUJOURS ignorer les requêtes OPTIONS (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return true;
        }
        
        // Endpoints publics (toutes méthodes HTTP)
        return path.startsWith("/api/v1/agence/auth/") ||
               path.startsWith("/api/v1/agence/health") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               
               // Endpoints GET publics
               (path.startsWith("/api/v1/agence/getAgences") && "GET".equals(method)) ||
               
               // Endpoints POST publics
               (path.startsWith("/api/v1/agence/add") && "POST".equals(method)) ||
               (path.startsWith("/api/v1/agence/register") && "POST".equals(method)) ||
               (path.startsWith("/api/v1/agence/contact") && "POST".equals(method)) ||
               (path.startsWith("/api/v1/agence/public/") && "POST".equals(method));
    }
}