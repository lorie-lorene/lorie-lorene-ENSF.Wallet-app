package com.serviceAgence.security;

import com.serviceAgence.model.AgenceUser;
import com.serviceAgence.repository.AgenceUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service de chargement des détails utilisateur pour Spring Security
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AgenceUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("🔍 Chargement utilisateur: {}", username);
        
        AgenceUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ Utilisateur introuvable: {}", username);
                    return new UsernameNotFoundException("Utilisateur introuvable: " + username);
                });

        // Vérification statut utilisateur
        boolean accountNonLocked = !user.isAccountLocked();
        boolean enabled = user.getStatus().name().equals("ACTIVE");

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                        .collect(Collectors.toList()))
                .accountExpired(false)
                .accountLocked(!accountNonLocked)
                .credentialsExpired(user.getPasswordExpired())
                .disabled(!enabled)
                .build();
    }
}