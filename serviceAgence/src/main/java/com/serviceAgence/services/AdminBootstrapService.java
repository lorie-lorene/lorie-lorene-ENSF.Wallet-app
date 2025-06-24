package com.serviceAgence.services;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import com.serviceAgence.model.AgenceUser;
import com.serviceAgence.repository.AgenceUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Service de création sécurisée du premier administrateur
 * - Mot de passe généré aléatoirement et affiché UNE SEULE FOIS
 * - Création uniquement si aucun admin n'existe
 * - Credentials depuis variables d'environnement ou générés
 */
@Service
@Slf4j
public class AdminBootstrapService implements CommandLineRunner {

    @Autowired
    private AgenceUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void run(String... args) throws Exception {
        createFirstAdminIfNeeded();
    }

    /**
     * Création du premier administrateur seulement si aucun admin n'existe
     */
    private void createFirstAdminIfNeeded() {
        // Vérifier si un admin existe déjà
        boolean adminExists = userRepository.findByRolesContaining(UserRole.ADMIN)
                .stream()
                .anyMatch(user -> user.getStatus() == UserStatus.ACTIVE);

        if (adminExists) {
            log.info("✅ Administrateur existe déjà - Bootstrap ignoré");
            return;
        }

        log.warn("🚨 AUCUN ADMINISTRATEUR TROUVÉ - Création automatique...");

        // Utiliser les variables d'environnement ou générer
        String username = adminUsername.isEmpty() ? "admin" : adminUsername;
        String email = adminEmail.isEmpty() ? "admin@agence.local" : adminEmail;
        String password = adminPassword.isEmpty() ? generateSecurePassword() : adminPassword;

        // Créer l'administrateur
        AgenceUser admin = new AgenceUser();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNom("ADMIN");
        admin.setPrenom("Système");
        admin.setRoles(Set.of(UserRole.ADMIN));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setCreatedBy("SYSTEM_BOOTSTRAP");
        admin.setFirstLogin(true);
        admin.setPasswordExpired(true); // Force le changement au premier login

        userRepository.save(admin);

        // AFFICHAGE SÉCURISÉ - UNE SEULE FOIS
        log.warn("🔐 =====================================================");
        log.warn("🔐     PREMIER ADMINISTRATEUR CRÉÉ");
        log.warn("🔐 =====================================================");
        log.warn("🔐 Username: {}", username);
        log.warn("🔐 Email:    {}", email);
        log.warn("🔐 Password: {}", password);
        log.warn("🔐 =====================================================");
        log.warn("🔐 ⚠️  NOTEZ CE MOT DE PASSE - IL NE SERA PLUS AFFICHÉ");
        log.warn("🔐 ⚠️  CHANGEZ-LE IMMÉDIATEMENT APRÈS LA PREMIÈRE CONNEXION");
        log.warn("🔐 =====================================================");
    }

    /**
     * Génération d'un mot de passe sécurisé de 16 caractères
     */
    private String generateSecurePassword() {
        StringBuilder password = new StringBuilder(16);
        
        // Au moins 1 majuscule
        password.append(CHARS.charAt(RANDOM.nextInt(26)));
        // Au moins 1 minuscule  
        password.append(CHARS.charAt(26 + RANDOM.nextInt(26)));
        // Au moins 1 chiffre
        password.append(CHARS.charAt(52 + RANDOM.nextInt(10)));
        // Au moins 1 caractère spécial
        password.append(CHARS.charAt(62 + RANDOM.nextInt(8)));
        
        // Remplir le reste aléatoirement
        for (int i = 4; i < 16; i++) {
            password.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        
        // Mélanger
        return shuffleString(password.toString());
    }

    /**
     * Mélange les caractères d'une chaîne
     */
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}