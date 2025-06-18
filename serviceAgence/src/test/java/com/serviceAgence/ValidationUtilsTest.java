package com.serviceAgence;


import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.serviceAgence.utils.ValidationUtils;

class ValidationUtilsTest {

    private ValidationUtils validationUtils;

    @BeforeEach
    void setUp() {
        validationUtils = new ValidationUtils();
    }

    @Test
    void testIsValidCameroonianCNI() {
        // Valid CNIs
        assertTrue(validationUtils.isValidCameroonianCNI("12345678"));
        assertTrue(validationUtils.isValidCameroonianCNI("123456789"));
        assertTrue(validationUtils.isValidCameroonianCNI("123456789012"));
        assertTrue(validationUtils.isValidCameroonianCNI("  123456789  ")); // Avec espaces

        // Invalid CNIs
        assertFalse(validationUtils.isValidCameroonianCNI("1234567")); // Trop court
        assertFalse(validationUtils.isValidCameroonianCNI("1234567890123")); // Trop long
        assertFalse(validationUtils.isValidCameroonianCNI("ABC12345")); // Contient des lettres
        assertFalse(validationUtils.isValidCameroonianCNI(null));
        assertFalse(validationUtils.isValidCameroonianCNI(""));
    }

    @Test
    void testIsValidCameroonianPhone() {
        // Valid phones
        assertTrue(validationUtils.isValidCameroonianPhone("655123456"));
        assertTrue(validationUtils.isValidCameroonianPhone("670123456"));
        assertTrue(validationUtils.isValidCameroonianPhone("680123456"));
        assertTrue(validationUtils.isValidCameroonianPhone("690123456"));

        // Invalid phones
        assertFalse(validationUtils.isValidCameroonianPhone("6541234566")); // Commence par 654
        assertFalse(validationUtils.isValidCameroonianPhone("755123456")); // Commence par 7
        assertFalse(validationUtils.isValidCameroonianPhone("65512345")); // Trop court
        assertFalse(validationUtils.isValidCameroonianPhone("6551234567")); // Trop long
        assertFalse(validationUtils.isValidCameroonianPhone(null));
        assertFalse(validationUtils.isValidCameroonianPhone(""));
    }

    @Test
    void testIsValidEmail() {
        // Valid emails
        assertTrue(validationUtils.isValidEmail("test@example.com"));
        assertTrue(validationUtils.isValidEmail("user.name@domain.co.uk"));
        assertTrue(validationUtils.isValidEmail("user+tag@example.org"));

        // Invalid emails
        assertFalse(validationUtils.isValidEmail("test@"));
        assertFalse(validationUtils.isValidEmail("@example.com"));
        assertFalse(validationUtils.isValidEmail("test.example.com"));
        assertFalse(validationUtils.isValidEmail(null));
        assertFalse(validationUtils.isValidEmail(""));
    }

    @Test
    void testIsValidAmount() {
        // Valid amounts
        assertTrue(validationUtils.isValidAmount(new BigDecimal("100")));
        assertTrue(validationUtils.isValidAmount(new BigDecimal("0.01")));
        assertTrue(validationUtils.isValidAmount(new BigDecimal("1000000")));

        // Invalid amounts
        assertFalse(validationUtils.isValidAmount(new BigDecimal("0")));
        assertFalse(validationUtils.isValidAmount(new BigDecimal("-100")));
        assertFalse(validationUtils.isValidAmount(null));
    }

    @Test
    void testIsAmountInRange() {
        BigDecimal min = new BigDecimal("100");
        BigDecimal max = new BigDecimal("10000");

        // Valid amounts in range
        assertTrue(validationUtils.isAmountInRange(new BigDecimal("100"), min, max));
        assertTrue(validationUtils.isAmountInRange(new BigDecimal("5000"), min, max));
        assertTrue(validationUtils.isAmountInRange(new BigDecimal("10000"), min, max));

        // Invalid amounts out of range
        assertFalse(validationUtils.isAmountInRange(new BigDecimal("99"), min, max));
        assertFalse(validationUtils.isAmountInRange(new BigDecimal("10001"), min, max));
        assertFalse(validationUtils.isAmountInRange(new BigDecimal("0"), min, max));
        assertFalse(validationUtils.isAmountInRange(null, min, max));
    }

    @Test
    void testIsValidImageFormat() {
        // Valid JPEG
        byte[] jpegImage = {(byte) 0xFF, (byte) 0xD8, 0x00, 0x00}; // Header JPEG
        byte[] largeJpegImage = new byte[2000];
        System.arraycopy(jpegImage, 0, largeJpegImage, 0, jpegImage.length);
        assertTrue(validationUtils.isValidImageFormat(largeJpegImage));

        // Valid PNG
        byte[] pngImage = {(byte) 0x89, 0x50, 0x4E, 0x47}; // Header PNG
        byte[] largePngImage = new byte[2000];
        System.arraycopy(pngImage, 0, largePngImage, 0, pngImage.length);
        assertTrue(validationUtils.isValidImageFormat(largePngImage));

        // Invalid formats
        byte[] invalidImage = {0x00, 0x00, 0x00, 0x00};
        byte[] largeInvalidImage = new byte[2000];
        System.arraycopy(invalidImage, 0, largeInvalidImage, 0, invalidImage.length);
        assertFalse(validationUtils.isValidImageFormat(largeInvalidImage));

        // Invalid sizes
        assertFalse(validationUtils.isValidImageFormat(new byte[500])); // Trop petit
        assertFalse(validationUtils.isValidImageFormat(null));
    }
}