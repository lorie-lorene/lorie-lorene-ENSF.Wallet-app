package com.serviceAgence.services;

import com.serviceAgence.dto.auth.*;
import com.serviceAgence.enums.UserStatus;
import com.serviceAgence.exception.AuthenticationException;
import com.serviceAgence.model.AgenceUser;
import com.serviceAgence.repository.AgenceUserRepository;
import com.serviceAgence.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service d'authentification pour AgenceService
 * Gère login, logout, refresh tokens et changement de mot de passe
 */
@Service
@Transactional
@Slf4j
public class AuthenticationService {
    
    @Autowired
    private AgenceUserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 jours par défaut
    private long refreshTokenExpirationMs;
    
    /**
     * Authentification utilisateur et génération tokens
     */
    public LoginResponse login(LoginRequest request, String ipAddress) {
        log.info("🔐 Tentative de connexion: {}", request.getUsername());
        
        try {
            // 1. Récupération utilisateur
            AgenceUser user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable"));
            
            // 2. Vérifications préliminaires
            validateUserForLogin(user);
            
            // 3. Vérification mot de passe
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                user.markFailedLogin();
                userRepository.save(user);
                throw new BadCredentialsException("Mot de passe incorrect");
            }
            
            // 4. Authentification Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword(),
                            user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                                    .collect(Collectors.toList())
                    )
            );
            
            // 5. Génération tokens
            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = generateRefreshToken();
            
            // 6. Mise à jour utilisateur
            user.updateLastLogin(ipAddress);
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
            userRepository.save(user);
            
            // 7. Construction réponse
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setExpiresIn(jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime() / 1000);
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setNom(user.getNom());
            response.setPrenom(user.getPrenom());
            response.setRoles(user.getRoles());
            response.setIdAgence(user.getIdAgence());
            response.setNomAgence(user.getNomAgence());
            response.setLoginTime(LocalDateTime.now());
            response.setFirstLogin(user.getFirstLogin());
            response.setPasswordExpired(user.getPasswordExpired());
            
            log.info("✅ Connexion réussie: {} - IP: {}", user.getUsername(), ipAddress);
            return response;
            
        } catch (Exception e) {
            log.error("❌ Échec connexion: {} - {}", request.getUsername(), e.getMessage());
            throw new AuthenticationException("Échec de l'authentification: " + e.getMessage());
        }
    }
    
    /**
     * Renouvellement du token d'accès
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.info("🔄 Demande renouvellement token");
        
        AgenceUser user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException("Refresh token invalide"));
        
        if (!user.isRefreshTokenValid()) {
            throw new AuthenticationException("Refresh token expiré");
        }
        
        // Génération nouveau access token
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toList());
        
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, authorities);
        String newAccessToken = jwtTokenProvider.generateToken(auth);
        
        // Construction réponse
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(request.getRefreshToken()); // Même refresh token
        response.setExpiresIn(jwtTokenProvider.getExpirationDateFromToken(newAccessToken).getTime() / 1000);
        response.setUsername(user.getUsername());
        response.setRoles(user.getRoles());
        
        log.info("✅ Token renouvelé: {}", user.getUsername());
        return response;
    }
    
    /**
     * Déconnexion utilisateur
     */
    public void logout(String username) {
        log.info("👋 Déconnexion: {}", username);
        
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        });
    }
    
    /**
     * Changement de mot de passe
     */
    public void changePassword(String username, ChangePasswordRequest request) {
        log.info("🔑 Changement mot de passe: {}", username);
        
        AgenceUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable"));
        
        // Vérification mot de passe actuel
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationException("Mot de passe actuel incorrect");
        }
        
        // Vérification confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Confirmation mot de passe invalide");
        }
        
        // Mise à jour
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpired(false);
        userRepository.save(user);
        
        log.info("✅ Mot de passe changé: {}", username);
    }
    
    /**
     * Validation utilisateur pour connexion
     */
    private void validateUserForLogin(AgenceUser user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Compte non actif");
        }
        
        if (user.isAccountLocked()) {
            throw new AuthenticationException("Compte temporairement verrouillé");
        }
    }
    
    /**
     * Génération refresh token sécurisé
     */
    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               System.currentTimeMillis();
    }

    /**
     * Récupération des informations utilisateur connecté
     */
    public UserInfoResponse getCurrentUserInfo(String username) {
        log.info("ℹ️ Récupération infos utilisateur: {}", username);
        
        AgenceUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable"));
        
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setIdAgence(user.getIdAgence());
        response.setNomAgence(user.getNomAgence());
        response.setRoles(user.getRoles());
        response.setStatus(user.getStatus());
        response.setLastLogin(user.getLastLogin());
        response.setLastLoginIp(user.getLastLoginIp());
        response.setFirstLogin(user.getFirstLogin());
        response.setPasswordExpired(user.getPasswordExpired());
        response.setPasswordChangedAt(user.getPasswordChangedAt());
        
        return response;
    }

    /**
     * Validation d'un token JWT
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
}