package com.serviceAgence.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Service de vérification faciale et qualité d'image
 * Fournit des métriques de base pour aider l'admin dans sa décision
 */
@Service
@Slf4j
public class FacialVerificationService {

    private static final int MIN_IMAGE_WIDTH = 300;
    private static final int MIN_IMAGE_HEIGHT = 300;
    private static final int MIN_FILE_SIZE = 20 * 1024; // 20KB
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Analyse complète du selfie utilisateur
     */
    public SelfieAnalysisResult analyzeSelfie(byte[] selfieImage, byte[] cniRectoImage) {
        log.info("🔍 Analyse du selfie - Taille: {} bytes", selfieImage.length);

        SelfieAnalysisResult result = new SelfieAnalysisResult();
        
        try {
            // 1. Validation de base du selfie
            result.setQualityScore(calculateImageQuality(selfieImage));
            result.setAnomalies(detectSelfieAnomalies(selfieImage));
            
            // 2. Détection de vie basique (métadonnées et patterns)
            result.setLivenessDetected(detectBasicLiveness(selfieImage));
            
            // 3. Comparaison faciale basique (si CNI disponible)
            if (cniRectoImage != null && cniRectoImage.length > 0) {
                result.setSimilarityScore(calculateBasicSimilarity(selfieImage, cniRectoImage));
                result.setFacialMatchRecommendation(generateFacialRecommendation(result.getSimilarityScore()));
            } else {
                result.setSimilarityScore(0);
                result.setFacialMatchRecommendation("Impossible de comparer - CNI non disponible");
            }
            
            // 4. Recommandation globale
            result.setOverallRecommendation(generateOverallRecommendation(result));
            
            log.info("✅ Analyse selfie terminée - Qualité: {}, Similarité: {}, Vie: {}", 
                    result.getQualityScore(), result.getSimilarityScore(), result.isLivenessDetected());
            
        } catch (Exception e) {
            log.error("❌ Erreur analyse selfie: {}", e.getMessage(), e);
            result.setQualityScore(0);
            result.setSimilarityScore(0);
            result.setLivenessDetected(false);
            result.setOverallRecommendation("Erreur d'analyse - Vérification manuelle requise");
            result.getAnomalies().add("ANALYSIS_ERROR");
        }
        
        return result;
    }

    /**
     * Calcul de la qualité d'image (0-100)
     */
    private int calculateImageQuality(byte[] imageData) {
        try {
            int score = 100;
            
            // 1. Vérification taille fichier
            if (imageData.length < MIN_FILE_SIZE) {
                score -= 30;
                log.debug("🔍 Fichier trop petit: {} bytes", imageData.length);
            }
            if (imageData.length > MAX_FILE_SIZE) {
                score -= 20;
                log.debug("🔍 Fichier trop grand: {} bytes", imageData.length);
            }
            
            // 2. Vérification dimensions image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                
                if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
                    score -= 25;
                    log.debug("🔍 Résolution insuffisante: {}x{}", width, height);
                }
                
                // 3. Vérification ratio d'aspect (approximativement carré pour un selfie)
                double ratio = (double) width / height;
                if (ratio < 0.75 || ratio > 1.33) {
                    score -= 10;
                    log.debug("🔍 Ratio d'aspect non optimal: {}", ratio);
                }
            } else {
                score -= 50;
                log.warn("🔍 Impossible de décoder l'image");
            }
            
            // 4. Vérification format (basique)
            if (!isValidImageFormat(imageData)) {
                score -= 15;
            }
            
            return Math.max(0, score);
            
        } catch (Exception e) {
            log.error("❌ Erreur calcul qualité image: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Détection d'anomalies dans le selfie
     */
    private List<String> detectSelfieAnomalies(byte[] imageData) {
        List<String> anomalies = new ArrayList<>();
        
        try {
            // 1. Vérification format d'image
            if (!isValidImageFormat(imageData)) {
                anomalies.add("FORMAT_INVALIDE");
            }
            
            // 2. Vérification taille
            if (imageData.length < MIN_FILE_SIZE) {
                anomalies.add("TAILLE_INSUFFISANTE");
            }
            if (imageData.length > MAX_FILE_SIZE) {
                anomalies.add("FICHIER_TROP_VOLUMINEUX");
            }
            
            // 3. Vérification contenu basique
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                anomalies.add("IMAGE_CORROMPUE");
            } else {
                // Vérifications additionnelles
                if (image.getWidth() < MIN_IMAGE_WIDTH || image.getHeight() < MIN_IMAGE_HEIGHT) {
                    anomalies.add("RESOLUTION_INSUFFISANTE");
                }
                
                // Détection de luminosité extrême (image trop sombre/claire)
                if (isImageTooDarkOrBright(image)) {
                    anomalies.add("LUMINOSITE_PROBLEMATIQUE");
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur détection anomalies: {}", e.getMessage());
            anomalies.add("ERREUR_ANALYSE");
        }
        
        return anomalies;
    }

    /**
     * Détection de vie basique (anti-spoofing simple)
     */
    private boolean detectBasicLiveness(byte[] imageData) {
        try {
            // NOTE: Ceci est une implémentation basique
            // En production, utiliser des services spécialisés (AWS Rekognition, Azure Face, etc.)
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                return false;
            }
            
            // 1. Vérification que l'image n'est pas complètement uniforme (écran)
            if (isImageTooUniform(image)) {
                log.debug("🔍 Image trop uniforme - possible écran");
                return false;
            }
            
            // 2. Vérification de la variabilité des couleurs
            if (!hasNaturalColorVariation(image)) {
                log.debug("🔍 Variation de couleurs non naturelle");
                return false;
            }
            
            // 3. Métadonnées EXIF (si disponibles) - caméra vs écran
            // TODO: Implémenter analyse EXIF
            
            // Par défaut, considérer comme vivant si pas d'anomalies détectées
            return true;
            
        } catch (Exception e) {
            log.error("❌ Erreur détection de vie: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calcul de similarité faciale basique (0-100)
     */
    private int calculateBasicSimilarity(byte[] selfieImage, byte[] cniImage) {
        try {
            // NOTE: Implémentation basique pour démonstration
            // En production, utiliser des APIs spécialisées (AWS Rekognition, Azure Face API, etc.)
            
            BufferedImage selfie = ImageIO.read(new ByteArrayInputStream(selfieImage));
            BufferedImage cni = ImageIO.read(new ByteArrayInputStream(cniImage));
            
            if (selfie == null || cni == null) {
                log.warn("🔍 Impossible de charger les images pour comparaison");
                return 0;
            }
            
            // 1. Comparaison basique de dimensions relatives
            double sizeScore = compareDimensions(selfie, cni);
            
            // 2. Comparaison basique de luminosité moyenne
            double brightnessScore = compareBrightness(selfie, cni);
            
            // 3. Comparaison basique de distribution des couleurs
            double colorScore = compareColorDistribution(selfie, cni);
            
            // Score composite basique
            int similarity = (int) ((sizeScore + brightnessScore + colorScore) / 3.0 * 100);
            
            log.debug("🔍 Similarité calculée: {} (Dim: {}, Lum: {}, Couleur: {})", 
                    similarity, sizeScore, brightnessScore, colorScore);
            
            return Math.max(0, Math.min(100, similarity));
            
        } catch (Exception e) {
            log.error("❌ Erreur calcul similarité: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Génération de recommandation faciale
     */
    private String generateFacialRecommendation(int similarityScore) {
        if (similarityScore >= 80) {
            return "FORTE_CORRESPONDANCE - Très probable que ce soit la même personne";
        } else if (similarityScore >= 60) {
            return "CORRESPONDANCE_MODEREE - Similarités notables, vérification recommandée";
        } else if (similarityScore >= 40) {
            return "CORRESPONDANCE_FAIBLE - Différences significatives, attention requise";
        } else if (similarityScore >= 20) {
            return "PEU_DE_CORRESPONDANCE - Très peu de similarités, vérification approfondie requise";
        } else {
            return "AUCUNE_CORRESPONDANCE - Personnes probablement différentes";
        }
    }

    /**
     * Recommandation globale pour l'admin
     */
    private String generateOverallRecommendation(SelfieAnalysisResult result) {
        if (result.getQualityScore() < 30) {
            return "REJET_RECOMMANDE - Qualité d'image insuffisante";
        }
        
        if (!result.isLivenessDetected()) {
            return "ATTENTION - Possible photo d'écran ou document, vérification manuelle critique";
        }
        
        if (result.getSimilarityScore() >= 70 && result.getQualityScore() >= 60) {
            return "APPROBATION_RECOMMANDEE - Correspondance faciale et qualité satisfaisantes";
        } else if (result.getSimilarityScore() >= 50) {
            return "VERIFICATION_MANUELLE - Correspondance modérée, examen attentif requis";
        } else {
            return "ATTENTION_MAXIMALE - Faible correspondance faciale, vérification approfondie requise";
        }
    }

    // ==========================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ==========================================

    private boolean isValidImageFormat(byte[] imageData) {
        // Vérification des magic numbers pour JPEG/PNG
        if (imageData.length < 4) return false;
        
        // JPEG: FF D8 FF
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF) {
            return true;
        }
        
        // PNG: 89 50 4E 47
        if (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && 
            imageData[2] == 0x4E && imageData[3] == 0x47) {
            return true;
        }
        
        return false;
    }

    private boolean isImageTooDarkOrBright(BufferedImage image) {
        try {
            long totalBrightness = 0;
            int pixelCount = 0;
            
            // Échantillonnage pour performance
            int stepX = Math.max(1, image.getWidth() / 100);
            int stepY = Math.max(1, image.getHeight() / 100);
            
            for (int x = 0; x < image.getWidth(); x += stepX) {
                for (int y = 0; y < image.getHeight(); y += stepY) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    totalBrightness += (r + g + b) / 3;
                    pixelCount++;
                }
            }
            
            double avgBrightness = (double) totalBrightness / pixelCount;
            
            // Trop sombre (< 30) ou trop clair (> 220)
            return avgBrightness < 30 || avgBrightness > 220;
            
        } catch (Exception e) {
            log.error("❌ Erreur analyse luminosité: {}", e.getMessage());
            return false;
        }
    }

    private boolean isImageTooUniform(BufferedImage image) {
        // Implémentation simplifiée - vérifier la variance des pixels
        try {
            int sampleSize = Math.min(1000, image.getWidth() * image.getHeight() / 10);
            int[] samples = new int[sampleSize];
            int index = 0;
            
            int stepX = Math.max(1, image.getWidth() / 50);
            int stepY = Math.max(1, image.getHeight() / 50);
            
            for (int x = 0; x < image.getWidth() && index < sampleSize; x += stepX) {
                for (int y = 0; y < image.getHeight() && index < sampleSize; y += stepY) {
                    int rgb = image.getRGB(x, y);
                    samples[index++] = rgb;
                }
            }
            
            // Calculer la variance
            double variance = calculateVariance(samples, index);
            
            // Image trop uniforme si variance très faible
            return variance < 1000; // Seuil à ajuster
            
        } catch (Exception e) {
            log.error("❌ Erreur analyse uniformité: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasNaturalColorVariation(BufferedImage image) {
        // Vérifier qu'il y a une variation naturelle des couleurs
        // Implémentation basique
        return true; // Placeholder
    }

    private double compareDimensions(BufferedImage img1, BufferedImage img2) {
        double ratio1 = (double) img1.getWidth() / img1.getHeight();
        double ratio2 = (double) img2.getWidth() / img2.getHeight();
        double difference = Math.abs(ratio1 - ratio2);
        return Math.max(0, 1.0 - difference);
    }

    private double compareBrightness(BufferedImage img1, BufferedImage img2) {
        // Comparaison basique de luminosité moyenne
        double brightness1 = calculateAverageBrightness(img1);
        double brightness2 = calculateAverageBrightness(img2);
        double difference = Math.abs(brightness1 - brightness2) / 255.0;
        return Math.max(0, 1.0 - difference);
    }

    private double compareColorDistribution(BufferedImage img1, BufferedImage img2) {
        // Implémentation basique de comparaison de distribution des couleurs
        return 0.6; // Placeholder - retourne score modéré
    }

    private double calculateAverageBrightness(BufferedImage image) {
        long totalBrightness = 0;
        int pixelCount = 0;
        
        int stepX = Math.max(1, image.getWidth() / 50);
        int stepY = Math.max(1, image.getHeight() / 50);
        
        for (int x = 0; x < image.getWidth(); x += stepX) {
            for (int y = 0; y < image.getHeight(); y += stepY) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                totalBrightness += (r + g + b) / 3;
                pixelCount++;
            }
        }
        
        return pixelCount > 0 ? (double) totalBrightness / pixelCount : 0;
    }

    private double calculateVariance(int[] values, int count) {
        if (count == 0) return 0;
        
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += values[i];
        }
        double mean = (double) sum / count;
        
        double variance = 0;
        for (int i = 0; i < count; i++) {
            variance += Math.pow(values[i] - mean, 2);
        }
        
        return variance / count;
    }
}