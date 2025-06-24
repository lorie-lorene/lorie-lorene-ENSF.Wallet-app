package com.serviceAgence.enums;

/**
 * Rôles disponibles dans le système AgenceService
 */
public enum UserRole {
    ADMIN("ROLE_ADMIN", "Administrateur système"),
    SUPERVISOR("ROLE_SUPERVISOR", "Superviseur agence"),
    AGENCE("ROLE_AGENCE", "Employé d'agence"),
    CLIENT("ROLE_CLIENT", "Client mobile");
    
    private final String authority;
    private final String description;
    
    UserRole(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }
    
    public String getAuthority() {
        return authority;
    }
    
    public String getDescription() {
        return description;
    }
}