package com.m1_fonda.serviceUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.response.RegisterResponse;
import com.m1_fonda.serviceUser.service.UserService;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;

/**
 * Tests unitaires pour l'enregistrement avec selfie
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Registration with Selfie Tests")
public class UserServiceRegistrationTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserServiceRabbit rabbit;

    @InjectMocks
    private UserService userService;

    private ClientRegistrationDTO validRegistrationWithSelfie;
    private String validSelfieBase64;

    @BeforeEach
    void setUp() {
        // Création d'un selfie Base64 valide pour les tests
        validSelfieBase64 = createValidSelfieBase64();
        
        validRegistrationWithSelfie = new ClientRegistrationDTO();
        validRegistrationWithSelfie.setCni("123456789012");
        validRegistrationWithSelfie.setEmail("test@example.com");
        validRegistrationWithSelfie.setNom("NGAMBA");
        validRegistrationWithSelfie.setPrenom("Jean");
        validRegistrationWithSelfie.setNumero("699123456");
        validRegistrationWithSelfie.setPassword("Password123!");
        validRegistrationWithSelfie.setIdAgence("AG001");
        validRegistrationWithSelfie.setRectoCni(createValidImageBase64());
        validRegistrationWithSelfie.setVersoCni(createValidImageBase64());
        validRegistrationWithSelfie.setSelfieImage(validSelfieBase64); // ← NEW: Selfie
    }

    @Test
    @DisplayName("Registration with valid selfie should succeed")
    void testRegistrationWithValidSelfie() {
        // Given
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.findByCni(anyString())).thenReturn(Optional.empty());
        when(repository.findByNumero(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        // When
        RegisterResponse response = userService.register(validRegistrationWithSelfie);

        // Then
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertTrue(response.getMessage().contains("transmise"));
        
        // Verify rabbit message was sent with selfie
        verify(rabbit, times(1)).sendRegistrationEvent(argThat(client -> 
            client.getSelfieImage() != null && 
            client.getSelfieImage().equals(validSelfieBase64)
        ));
    }

    @Test
    @DisplayName("Registration without selfie should fail")
    void testRegistrationWithoutSelfie() {
        // Given
        validRegistrationWithSelfie.setSelfieImage(null);

        // When & Then
        assertThrows(BusinessValidationException.class, () -> {
            userService.register(validRegistrationWithSelfie);
        }, "Should throw exception when selfie is missing");
    }

    @Test
    @DisplayName("Registration with invalid selfie format should fail")
    void testRegistrationWithInvalidSelfieFormat() {
        // Given
        validRegistrationWithSelfie.setSelfieImage("invalid-base64-data");

        // When & Then
        assertThrows(BusinessValidationException.class, () -> {
            userService.register(validRegistrationWithSelfie);
        }, "Should throw exception when selfie format is invalid");
    }

    @Test
    @DisplayName("Registration with selfie too small should fail")
    void testRegistrationWithSelfieTooSmall() {
        // Given - Create very small image (< 20KB)
        String smallSelfie = Base64.getEncoder().encodeToString("small".getBytes());
        validRegistrationWithSelfie.setSelfieImage(smallSelfie);

        // When & Then
        assertThrows(BusinessValidationException.class, () -> {
            userService.register(validRegistrationWithSelfie);
        }, "Should throw exception when selfie is too small");
    }

    // Helper methods for test data
    private String createValidSelfieBase64() {
        // Create a valid JPEG header + some data (> 20KB)
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] imageData = new byte[25000]; // 25KB
        System.arraycopy(jpegHeader, 0, imageData, 0, jpegHeader.length);
        
        // Fill rest with dummy data
        for (int i = jpegHeader.length; i < imageData.length; i++) {
            imageData[i] = (byte) (i % 256);
        }
        
        return Base64.getEncoder().encodeToString(imageData);
    }

    private String createValidImageBase64() {
        // Similar to selfie but for CNI images
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] imageData = new byte[60000]; // 60KB
        System.arraycopy(jpegHeader, 0, imageData, 0, jpegHeader.length);
        
        for (int i = jpegHeader.length; i < imageData.length; i++) {
            imageData[i] = (byte) (i % 256);
        }
        
        return Base64.getEncoder().encodeToString(imageData);
    }
}