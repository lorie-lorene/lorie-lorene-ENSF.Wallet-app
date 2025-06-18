package com.serviceAgence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.serviceAgence.dto.KYCValidationResult;
import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.enums.DocumentType;
import com.serviceAgence.model.DocumentKYC;
import com.serviceAgence.repository.DocumentKYCRepository;
import com.serviceAgence.services.KYCService;

@ExtendWith(MockitoExtension.class)
class KYCServiceTest {

    @Mock
    private DocumentKYCRepository documentRepository;

    @InjectMocks
    private KYCService kycService;

    private byte[] validJpegImage;
    private byte[] validPngImage;
    private byte[] invalidImage;
    private String validCNI;
    private String invalidCNI;

    @BeforeEach
    void setUp() {
        // Images valides (simulation des headers JPEG/PNG)
        validJpegImage = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00, 0x00}; // Header JPEG
        validPngImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // Header PNG
        
        // Image invalide
        invalidImage = new byte[]{0x00, 0x00, 0x00};
        
        // Ajouter de la taille pour passer la validation minimale
        validJpegImage = expandImageData(validJpegImage, 60000); // 60KB
        validPngImage = expandImageData(validPngImage, 80000); // 80KB
        invalidImage = expandImageData(invalidImage, 1500); // 1.5KB

        // CNI valides et invalides
        validCNI = "123456789";
        invalidCNI = "ABC123";
    }

    private byte[] expandImageData(byte[] header, int targetSize) {
        byte[] expanded = new byte[targetSize];
        System.arraycopy(header, 0, expanded, 0, Math.min(header.length, targetSize));
        // Remplir le reste avec des données aléatoires
        for (int i = header.length; i < targetSize; i++) {
            expanded[i] = (byte) (i % 256);
        }
        return expanded;
    }

    @Test
    void testValidateDocuments_Success() {
        // Given
        when(documentRepository.existsByNumeroDocumentAndType(validCNI, DocumentType.CNI_CAMEROUNAISE))
            .thenReturn(false);
        when(documentRepository.save(any(DocumentKYC.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KYCValidationResult result = kycService.validateDocuments(
            "CLIENT123", validCNI, validJpegImage, validPngImage);

        // Then
        assertTrue(result.isValid());
        assertEquals("DOCUMENTS_CONFORMES", result.getErrorCode());
        assertTrue(result.getReason().contains("validés avec succès"));
        
        verify(documentRepository).existsByNumeroDocumentAndType(validCNI, DocumentType.CNI_CAMEROUNAISE);
        verify(documentRepository).save(any(DocumentKYC.class));
    }

    @Test
    void testValidateDocuments_InvalidCNIFormat() {
        // When
        KYCValidationResult result = kycService.validateDocuments(
            "CLIENT123", invalidCNI, validJpegImage, validPngImage);

        // Then
        assertFalse(result.isValid());
        assertEquals("FORMAT_CNI_INCORRECT", result.getErrorCode());
        assertTrue(result.getReason().contains("format de la CNI"));
        
        verify(documentRepository, never()).save(any());
    }

    @Test
    void testValidateDocuments_CNIAlreadyUsed() {
        // Given
        when(documentRepository.existsByNumeroDocumentAndType(validCNI, DocumentType.CNI_CAMEROUNAISE))
            .thenReturn(true);

        // When
        KYCValidationResult result = kycService.validateDocuments(
            "CLIENT123", validCNI, validJpegImage, validPngImage);

        // Then
        assertFalse(result.isValid());
        assertEquals("CNI_DEJA_UTILISEE", result.getErrorCode());
        assertTrue(result.getReason().contains("déjà associée"));
        
        verify(documentRepository, never()).save(any());
    }

    @Test
    void testValidateDocuments_PoorImageQuality() {
        // Given
        byte[] smallImage = new byte[1000]; // Trop petit
        when(documentRepository.existsByNumeroDocumentAndType(validCNI, DocumentType.CNI_CAMEROUNAISE))
            .thenReturn(false);
        when(documentRepository.save(any(DocumentKYC.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KYCValidationResult result = kycService.validateDocuments(
            "CLIENT123", validCNI, smallImage, smallImage);

        // Then
        assertFalse(result.isValid());
        assertEquals("QUALITE_IMAGE_INSUFFISANTE", result.getErrorCode());
        assertTrue(result.getReason().contains("qualité des images"));
        
        verify(documentRepository).save(any(DocumentKYC.class));
    }

    @Test
    void testValidateDocuments_FraudDetected() {
        // Given
        String blacklistedCNI = "000000000"; // CNI dans la blacklist
        when(documentRepository.existsByNumeroDocumentAndType(blacklistedCNI, DocumentType.CNI_CAMEROUNAISE))
            .thenReturn(false);
        when(documentRepository.save(any(DocumentKYC.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KYCValidationResult result = kycService.validateDocuments(
            "CLIENT123", blacklistedCNI, validJpegImage, validPngImage);

        // Then
        assertFalse(result.isValid());
        assertEquals("FRAUDE_DETECTEE", result.getErrorCode());
        assertTrue(result.getReason().contains("Document suspect"));
        
        verify(documentRepository).save(any(DocumentKYC.class));
    }

    @Test
    void testGenerateKYCReport() {
        // Given
        DocumentKYC document1 = new DocumentKYC();
        document1.setType(DocumentType.CNI_CAMEROUNAISE);
        document1.setNumeroDocument("123456789");
        document1.setStatus(DocumentStatus.VALIDATED);
        document1.setScoreQualite(85);
        document1.setFraudDetected(false);

        DocumentKYC document2 = new DocumentKYC();
        document2.setType(DocumentType.PASSEPORT);
        document2.setNumeroDocument("PASS123456");
        document2.setStatus(DocumentStatus.REJECTED);
        document2.setScoreQualite(45);
        document2.setFraudDetected(true);
        document2.setAnomaliesDetectees(Arrays.asList("Image corrompue"));

        List<DocumentKYC> documents = Arrays.asList(document1, document2);
        
        when(documentRepository.findByIdClientOrderByUploadedAtDesc("CLIENT123"))
            .thenReturn(documents);

        // When
        String report = kycService.generateKYCReport("CLIENT123");

        // Then
        assertNotNull(report);
        assertTrue(report.contains("=== RAPPORT KYC CLIENT CLIENT123 ==="));
        assertTrue(report.contains("Carte Nationale d'Identité Camerounaise"));
        assertTrue(report.contains("123456789"));
        assertTrue(report.contains("Validé"));
        assertTrue(report.contains("Score qualité: 85/100"));
        assertTrue(report.contains("⚠️ FRAUDE DÉTECTÉE"));
        assertTrue(report.contains("Image corrompue"));
        
        verify(documentRepository).findByIdClientOrderByUploadedAtDesc("CLIENT123");
    }

    @Test
    void testGetClientDocuments() {
        // Given
        DocumentKYC document = new DocumentKYC();
        document.setIdClient("CLIENT123");
        document.setType(DocumentType.CNI_CAMEROUNAISE);
        document.setStatus(DocumentStatus.VALIDATED);
        
        List<DocumentKYC> documents = Arrays.asList(document);
        when(documentRepository.findByIdClientOrderByUploadedAtDesc("CLIENT123"))
            .thenReturn(documents);

        // When
        List<DocumentKYC> result = kycService.getClientDocuments("CLIENT123");

        // Then
        assertEquals(1, result.size());
        assertEquals(document, result.get(0));
        verify(documentRepository).findByIdClientOrderByUploadedAtDesc("CLIENT123");
    }
}

