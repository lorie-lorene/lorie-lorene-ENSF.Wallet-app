package com.m1_fonda.serviceUser.pojo;

public enum ClientStatus {
    PENDING, // En attente validation
    ACTIVE, // Compte actif
    SUSPENDED, // Suspendu temporairement
    BLOCKED, // Bloqué définitivement
    REJECTED // Demande rejetée
}