package com.serviceAgence;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.serviceAgence.utils.AccountNumberGenerator;

class AccountNumberGeneratorTest {

    private AccountNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AccountNumberGenerator();
    }

    @Test
    void testGenerateAccountNumber_Success() {
        // When
        Long accountNumber1 = generator.generateAccountNumber("CLIENT123", "AGENCE001");
        Long accountNumber2 = generator.generateAccountNumber("CLIENT456", "AGENCE001");
        Long accountNumber3 = generator.generateAccountNumber("CLIENT123", "AGENCE002");

        // Then
        assertNotNull(accountNumber1);
        assertNotNull(accountNumber2);
        assertNotNull(accountNumber3);
        
        // Tous les numéros doivent être différents
        assertNotEquals(accountNumber1, accountNumber2);
        assertNotEquals(accountNumber1, accountNumber3);
        assertNotEquals(accountNumber2, accountNumber3);
        
        // Tous doivent avoir au moins 8 chiffres
        assertTrue(accountNumber1 >= 10000000L);
        assertTrue(accountNumber2 >= 10000000L);
        assertTrue(accountNumber3 >= 10000000L);
    }

    @Test
    void testIsValidAccountNumber() {
        // Valid account numbers
        assertTrue(generator.isValidAccountNumber("123456789"));
        assertTrue(generator.isValidAccountNumber("10000000"));
        assertTrue(generator.isValidAccountNumber("999999999999"));

        // Invalid account numbers
        assertFalse(generator.isValidAccountNumber("1234567")); // Trop court
        assertFalse(generator.isValidAccountNumber("ABC123456")); // Contient des lettres
        assertFalse(generator.isValidAccountNumber(null));
        assertFalse(generator.isValidAccountNumber(""));
        assertFalse(generator.isValidAccountNumber("   "));
    }
}

