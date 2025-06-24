package com.serviceAgence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.serviceAgence.dto.KYCValidationResult;
import com.serviceAgence.repository.DocumentKYCRepository;
import com.serviceAgence.services.KYCService;

/**
 * Tests unitaires pour la validation KYC avec selfie
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KYC Service Tests")
public class KYCServiceTest {

    @Mock
    private DocumentKYCRepository documentRepository;

    @InjectMocks
    private KYCService kycService;

    private byte[] validRectoImage;
    private byte[] validVersoImage;
    //private byte[] invalidImage;

    @BeforeEach
    void setUp() {
        validRectoImage = createValidJPEGImage(60000); // 60KB
        validVersoImage = createValidJPEGImage(55000); // 55KB
        //invalidImage = "invalid".getBytes(); // Invalid image
    }

    @Test
    @DisplayName("Basic validation with valid images should succeed")
    void testValidateDocumentsBasic_WithValidImages_ShouldSucceed() {
        // When
        KYCValidationResult result = kycService.validateDocumentsBasic(
            "CLIENT123", "123456789012", validRectoImage, validVersoImage);

        // Then
        assertTrue(result.isValid());
        assertEquals("VALIDATION_BASIQUE_OK", result.getErrorCode());
        assertTrue(result.getQualityScore() > 70);
        assertNotNull(result.getAnomalies());
        assertTrue(result.getDocumentsValidated().contains("CNI_RECTO"));
        assertTrue(result.getDocumentsValidated().contains("CNI_VERSO"));
    }

    @Test
    @DisplayName("Basic validation with invalid CNI format should fail")
    void testValidateDocumentsBasic_WithInvalidCNI_ShouldFail() {
        // When
        KYCValidationResult result = kycService.validateDocumentsBasic(
            "CLIENT123", "invalid-cni", validRectoImage, validVersoImage);

        // Then
        assertFalse(result.isValid());
        assertEquals("FORMAT_CNI_INCORRECT", result.getErrorCode());
        assertEquals(0, result.getQualityScore());
    }

    @Test
    @DisplayName("Basic validation with missing images should fail")
    void testValidateDocumentsBasic_WithMissingImages_ShouldFail() {
        // When
        KYCValidationResult result = kycService.validateDocumentsBasic(
            "CLIENT123", "123456789012", null, validVersoImage);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().contains("IMAGE_RECTO_MANQUANTE"));
        assertTrue(result.getQualityScore() < 70);
    }

    @Test
    @DisplayName("Basic validation with poor quality images should fail")
    void testValidateDocumentsBasic_WithPoorQuality_ShouldFail() {
        // Given - very small images
        byte[] smallImage = createValidJPEGImage(10000); // 10KB (too small)

        // When
        KYCValidationResult result = kycService.validateDocumentsBasic(
            "CLIENT123", "123456789012", smallImage, smallImage);

        // Then
        assertFalse(result.isValid());
        assertEquals("QUALITE_INSUFFISANTE", result.getErrorCode());
        assertTrue(result.getAnomalies().contains("IMAGE_RECTO_TROP_PETITE"));
        assertTrue(result.getAnomalies().contains("IMAGE_VERSO_TROP_PETITE"));
    }

    // Helper method to create valid JPEG images for testing
    private byte[] createValidJPEGImage(int size) {
        byte[] image = new byte[size];
        // JPEG header
        image[0] = (byte) 0xFF;
        image[1] = (byte) 0xD8;
        image[2] = (byte) 0xFF;
        image[3] = (byte) 0xE0;
        
        // Fill rest with dummy data
        for (int i = 4; i < size; i++) {
            image[i] = (byte) (i % 256);
        }
        
        return image;
    }
}