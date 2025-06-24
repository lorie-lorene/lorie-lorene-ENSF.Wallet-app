package com.serviceAgence;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.serviceAgence.dto.KYCValidationResult;
import com.serviceAgence.services.FacialVerificationService;
import com.serviceAgence.services.KYCService;
import com.serviceAgence.services.SelfieAnalysisResult;

/**
 * Simple test runner for manual verification
 * Enable with: app.testing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
public class SimpleTestRunner implements CommandLineRunner {

    @Autowired
    private KYCService kycService;

    @Autowired
    private FacialVerificationService facialVerificationService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n🧪 STARTING SIMPLE SYSTEM TESTS\n");
        
        // Test 1: KYC Basic Validation
        testKYCBasicValidation();
        
        // Test 2: Facial Verification
        testFacialVerification();
        
        System.out.println("\n✅ SIMPLE TESTS COMPLETED\n");
    }

    private void testKYCBasicValidation() {
        System.out.println("🔍 Testing KYC Basic Validation...");
        
        try {
            // Create test images
            byte[] rectoImage = createTestImage(60000);
            byte[] versoImage = createTestImage(55000);
            
            // Test validation
            KYCValidationResult result = kycService.validateDocumentsBasic(
                "TEST_CLIENT", "123456789012", rectoImage, versoImage);
            
            System.out.println("  ✅ KYC Validation Result:");
            System.out.println("    Valid: " + result.isValid());
            System.out.println("    Quality Score: " + result.getQualityScore() + "%");
            System.out.println("    Anomalies: " + result.getAnomalies().size());
            System.out.println("    Error Code: " + result.getErrorCode());
            
        } catch (Exception e) {
            System.out.println("  ❌ KYC Test Failed: " + e.getMessage());
        }
    }

    private void testFacialVerification() {
        System.out.println("\n📸 Testing Facial Verification...");
        
        try {
            // Create test images
            byte[] selfieImage = createTestImage(30000);
            byte[] cniImage = createTestImage(60000);
            
            // Test facial verification
            SelfieAnalysisResult result = facialVerificationService.analyzeSelfie(selfieImage, cniImage);
            
            System.out.println("  ✅ Facial Verification Result:");
            System.out.println("    Quality Score: " + result.getQualityScore() + "%");
            System.out.println("    Similarity Score: " + result.getSimilarityScore() + "%");
            System.out.println("    Liveness Detected: " + result.isLivenessDetected());
            System.out.println("    Anomalies: " + result.getAnomalies().size());
            System.out.println("    Recommendation: " + result.getOverallRecommendation());
            
        } catch (Exception e) {
            System.out.println("  ❌ Facial Verification Test Failed: " + e.getMessage());
        }
    }

    private byte[] createTestImage(int size) {
        byte[] image = new byte[size];
        // JPEG header
        image[0] = (byte) 0xFF;
        image[1] = (byte) 0xD8;
        image[2] = (byte) 0xFF;
        image[3] = (byte) 0xE0;
        
        // Fill with test data
        for (int i = 4; i < size; i++) {
            image[i] = (byte) ((i * 13 + 42) % 256);
        }
        
        return image;
    }
}