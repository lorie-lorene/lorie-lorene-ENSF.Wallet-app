package com.serviceAgence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.serviceAgence.services.FacialVerificationService;
import com.serviceAgence.services.SelfieAnalysisResult;

/**
 * Tests unitaires pour la vérification faciale
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Facial Verification Service Tests")
public class FacialVerificationServiceTest {

    @InjectMocks
    private FacialVerificationService facialVerificationService;

    private byte[] validSelfie;
    private byte[] validCniRecto;
    private byte[] invalidImage;

    @BeforeEach
    void setUp() {
        validSelfie = createValidSelfieImage(30000); // 30KB
        validCniRecto = createValidJPEGImage(60000); // 60KB
        invalidImage = "invalid".getBytes();
    }

    @Test
    @DisplayName("Analyze valid selfie should return positive results")
    void testAnalyzeSelfie_WithValidImages_ShouldSucceed() {
        // When
        SelfieAnalysisResult result = facialVerificationService.analyzeSelfie(validSelfie, validCniRecto);

        // Then
        assertNotNull(result);
        assertTrue(result.getQualityScore() > 0);
        assertTrue(result.getSimilarityScore() >= 0);
        assertNotNull(result.getOverallRecommendation());
        assertNotNull(result.getAnomalies());
        
        System.out.println("🔍 Selfie Analysis Results:");
        System.out.println("  Quality Score: " + result.getQualityScore() + "%");
        System.out.println("  Similarity Score: " + result.getSimilarityScore() + "%");
        System.out.println("  Liveness Detected: " + result.isLivenessDetected());
        System.out.println("  Recommendation: " + result.getOverallRecommendation());
    }

    @Test
    @DisplayName("Analyze invalid selfie should return low scores")
    void testAnalyzeSelfie_WithInvalidImage_ShouldReturnLowScores() {
        // When
        SelfieAnalysisResult result = facialVerificationService.analyzeSelfie(invalidImage, validCniRecto);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getQualityScore());
        assertEquals(0, result.getSimilarityScore());
        assertFalse(result.isLivenessDetected());
        assertTrue(result.getAnomalies().contains("ANALYSIS_ERROR"));
    }

    @Test
    @DisplayName("Analyze selfie without CNI should handle gracefully")
    void testAnalyzeSelfie_WithoutCNI_ShouldHandleGracefully() {
        // When
        SelfieAnalysisResult result = facialVerificationService.analyzeSelfie(validSelfie, null);

        // Then
        assertNotNull(result);
        assertTrue(result.getQualityScore() > 0); // Quality should still be analyzed
        assertEquals(0, result.getSimilarityScore()); // No similarity without CNI
        assertTrue(result.getFacialMatchRecommendation().contains("CNI non disponible"));
    }

    // Helper methods
    private byte[] createValidSelfieImage(int size) {
        return createValidJPEGImage(size);
    }

    private byte[] createValidJPEGImage(int size) {
        byte[] image = new byte[size];
        // JPEG header
        image[0] = (byte) 0xFF;
        image[1] = (byte) 0xD8;
        image[2] = (byte) 0xFF;
        image[3] = (byte) 0xE0;
        
        // Fill with varied data to simulate real image
        for (int i = 4; i < size; i++) {
            image[i] = (byte) ((i * 7 + 123) % 256); // More varied pattern
        }
        
        return image;
    }
}