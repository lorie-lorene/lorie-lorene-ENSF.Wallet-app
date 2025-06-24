package com.serviceAgence.services;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.enums.DocumentType;
import com.serviceAgence.model.DocumentKYC;
import com.serviceAgence.repository.DocumentKYCRepository;
import com.serviceAgence.dto.KYCValidationResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class KYCService {

    @Autowired
    private DocumentKYCRepository documentRepository;

    private static final Pattern CNI_PATTERN = Pattern.compile("\\d{8,12}");
    private static final int MIN_QUALITY_SCORE = 70;

    /**
     * Validation complète des documents KYC
     */
    public KYCValidationResult validateDocuments(String idClient, String cni, 
                                                byte[] rectoImage, byte[] versoImage) {
        log.info("Début validation KYC pour client: {}", idClient);

        try {
            // 1. Validation format CNI
            if (!isValidCameroonianCNI(cni)) {
                return KYCValidationResult.rejected("FORMAT_CNI_INCORRECT", 
                    "Le format de la CNI camerounaise est incorrect");
            }

            // 2. Vérification doublon
            if (documentRepository.existsByNumeroDocumentAndType(cni, DocumentType.CNI_CAMEROUNAISE)) {
                return KYCValidationResult.rejected("CNI_DEJA_UTILISEE", 
                    "Cette CNI est déjà associée à un autre compte");
            }

            // 3. Validation qualité des images
            DocumentKYC document = new DocumentKYC();
            document.setIdClient(idClient);
            document.setType(DocumentType.CNI_CAMEROUNAISE);
            document.setNumeroDocument(cni);

            // Validation qualité technique
            int qualityScore = validateImageQuality(rectoImage, versoImage);
            document.setScoreQualite(qualityScore);

            if (qualityScore < MIN_QUALITY_SCORE) {
                document.setStatus(DocumentStatus.REJECTED);
                document.setRejectionReason("Qualité d'image insuffisante");
                documentRepository.save(document);
                
                return KYCValidationResult.rejected("QUALITE_IMAGE_INSUFFISANTE", 
                    "La qualité des images est insuffisante (score: " + qualityScore + "/100)");
            }

            // 4. Détection de fraude
            List<String> anomalies = detectFraud(document, rectoImage, versoImage);
            if (!anomalies.isEmpty()) {
                document.markAsFraudulent(anomalies);
                documentRepository.save(document);
                
                return KYCValidationResult.rejected("FRAUDE_DETECTEE", 
                    "Document suspect: " + String.join(", ", anomalies));
            }

            // 5. Extraction données (simulation OCR)
            extractDocumentData(document, rectoImage);

            // 6. Sauvegarde document validé
            document.setStatus(DocumentStatus.APPROVED);
            document.setValidatedAt(LocalDateTime.now());
            document.setValidatedBy("SYSTEM_KYC");
            documentRepository.save(document);

            log.info("Validation KYC réussie pour client: {}", idClient);
            return KYCValidationResult.accepted("DOCUMENTS_CONFORMES", 
                "Documents validés avec succès");

        } catch (Exception e) {
            log.error("Erreur lors de la validation KYC: {}", e.getMessage(), e);
            return KYCValidationResult.rejected("ERREUR_TECHNIQUE", 
                "Erreur technique lors de la validation");
        }
    }

    /**
     * Validation du format CNI camerounaise
     */
    private boolean isValidCameroonianCNI(String cni) {
        if (cni == null || cni.trim().isEmpty()) {
            return false;
        }
        
        String cleanCni = cni.trim().replaceAll("\\s+", "");
        return CNI_PATTERN.matcher(cleanCni).matches() && 
               cleanCni.length() >= 8 && cleanCni.length() <= 12;
    }

    /**
     * Validation de la qualité des images
     */
    private int validateImageQuality(byte[] rectoImage, byte[] versoImage) {
        int score = 100;

        // Validation taille minimale
        if (rectoImage == null || rectoImage.length < 50000) { // 50KB min
            score -= 30;
        }
        if (versoImage == null || versoImage.length < 50000) {
            score -= 30;
        }

        // Validation taille maximale
        if (rectoImage != null && rectoImage.length > 10000000) { // 10MB max
            score -= 20;
        }
        if (versoImage != null && versoImage.length > 10000000) {
            score -= 20;
        }

        // TODO: Ajouter validation avec bibliothèque image processing
        // - Résolution minimale
        // - Netteté
        // - Luminosité
        // - Détection de flou

        return Math.max(0, score);
    }

    /**
     * Détection de fraude documentaire
     */
    private List<String> detectFraud(DocumentKYC document, byte[] rectoImage, byte[] versoImage) {
        List<String> anomalies = new ArrayList<>();

        // Vérification liste noire CNI
        if (isBlacklistedCNI(document.getNumeroDocument())) {
            anomalies.add("CNI dans la liste noire");
        }

        // Détection format d'image suspect
        if (isCorruptedImage(rectoImage)) {
            anomalies.add("Image recto corrompue ou modifiée");
        }
        if (isCorruptedImage(versoImage)) {
            anomalies.add("Image verso corrompue ou modifiée");
        }

        // TODO: Ajouter détections avancées
        // - Détection de copie/scan
        // - Analyse des métadonnées EXIF
        // - Détection de photomontage
        // - Comparaison avec base de données d'images connues

        return anomalies;
    }

    /**
     * Extraction des données du document (simulation OCR)
     */
    private void extractDocumentData(DocumentKYC document, byte[] rectoImage) {
        // TODO: Intégrer vraie solution OCR
        // Pour l'instant, simulation

        // Simulation extraction nom/prénom
        document.setNomExtrait("NOM_EXTRAIT_OCR");
        document.setPrenomExtrait("PRENOM_EXTRAIT_OCR");
        
        log.info("Données extraites du document {} : nom={}, prénom={}", 
                document.getNumeroDocument(), document.getNomExtrait(), document.getPrenomExtrait());
    }

    /**
     * Vérification liste noire CNI
     */
    private boolean isBlacklistedCNI(String cni) {
        // TODO: Implémenter vérification base de données liste noire
        // Pour l'instant, quelques CNI de test
        List<String> blacklist = List.of("000000000", "111111111", "123456789");
        return blacklist.contains(cni);
    }

    /**
     * Détection d'image corrompue
     */
    private boolean isCorruptedImage(byte[] imageData) {
        if (imageData == null || imageData.length < 1000) {
            return true;
        }
        
        new String(imageData, 0, Math.min(10, imageData.length));
        boolean isValidJPEG = imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8;
        boolean isValidPNG = imageData[0] == (byte) 0x89 && imageData[1] == 0x50;
        
        return !(isValidJPEG || isValidPNG);
    }

    /**
     * Récupération des documents d'un client
     */
    public List<DocumentKYC> getClientDocuments(String idClient) {
        return documentRepository.findByIdClientOrderByUploadedAtDesc(idClient);
    }

    /**
     * Génération rapport KYC
     */
    public String generateKYCReport(String idClient) {
        List<DocumentKYC> documents = getClientDocuments(idClient);
        
        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT KYC CLIENT ").append(idClient).append(" ===\n");
        report.append("Date génération: ").append(LocalDateTime.now()).append("\n\n");
        
        for (DocumentKYC doc : documents) {
            report.append("Document: ").append(doc.getType().getDescription()).append("\n");
            report.append("Numéro: ").append(doc.getNumeroDocument()).append("\n");
            report.append("Statut: ").append(doc.getStatus().getDescription()).append("\n");
            report.append("Score qualité: ").append(doc.getScoreQualite()).append("/100\n");
            if (doc.getFraudDetected()) {
                report.append("⚠️ FRAUDE DÉTECTÉE: ").append(doc.getAnomaliesDetectees()).append("\n");
            }
            report.append("---\n");
        }
        
        return report.toString();
    }
}