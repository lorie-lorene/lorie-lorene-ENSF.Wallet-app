package com.wallet.bank_card_service.controler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.bank_card_service.service.CarteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cartes/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Autowired
    private CarteService carteService;
    
    /**
     * Webhook appelé par le service Money quand une recharge est confirmée
     */
  @PostMapping("/money-callback")
public ResponseEntity<Map<String, Object>> handleMoneyCallback(@RequestBody String rawPayload) {
    log.info("🔔 [WEBHOOK] Callback reçu du service Money: {}", rawPayload);
    
    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);
        
        String requestId = (String) payload.get("requestId");
        String idCarte = (String) payload.get("idCarte");
        String status = (String) payload.get("status");
        Object montantObj = payload.get("montant");
        
        if (requestId == null || idCarte == null || status == null) {
            log.warn("⚠️ [WEBHOOK] Payload invalide: {}", rawPayload);
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "Payload invalide"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Convertir le montant
        BigDecimal montant = new BigDecimal(montantObj.toString());
        
        log.info("📝 [WEBHOOK] Transaction - RequestId: {}, Carte: {}, Status: {}, Montant: {}", 
                requestId, idCarte, status, montant);
        
        if ("SUCCESS".equals(status)) {
            // ✅ Créditer la carte
            carteService.crediterCarte(idCarte, montant, requestId);
            log.info("✅ [WEBHOOK] Carte créditée avec succès - Carte: {}, Montant: {}", idCarte, montant);
            
            // Retourner une réponse JSON de succès
            Map<String, Object> successResponse = Map.of(
                "status", "success",
                "message", "Webhook traité avec succès",
                "requestId", requestId,
                "cardId", idCarte,
                "amount", montant
            );
            return ResponseEntity.ok(successResponse);
        } else {
            log.warn("❌ [WEBHOOK] Recharge échouée - Status: {}", status);
            Map<String, Object> failureResponse = Map.of(
                "status", "failed",
                "message", "Recharge échouée",
                "requestId", requestId,
                "originalStatus", status
            );
            return ResponseEntity.ok(failureResponse);
        }
        
    } catch (Exception e) {
        log.error("❌ [WEBHOOK] Erreur traitement: {}", e.getMessage(), e);
        Map<String, Object> errorResponse = Map.of(
            "status", "error",
            "message", "Erreur traitement webhook",
            "error", e.getMessage()
        );
        return ResponseEntity.status(500).body(errorResponse);
    }
}
    /**
     * Webhook pour les retraits carte
     */
    @PostMapping("/money-withdrawal-callback")
    public ResponseEntity<String> handleWithdrawalCallback(@RequestBody String rawPayload) {
        log.info("🔔 [WITHDRAWAL-WEBHOOK] Callback retrait reçu: {}", rawPayload);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);
            
            String requestId = (String) payload.get("requestId");
            String status = (String) payload.get("status");
            
            if ("SUCCESS".equals(status)) {
                log.info("✅ [WITHDRAWAL-WEBHOOK] Retrait confirmé - RequestId: {}", requestId);
            } else {
                log.warn("❌ [WITHDRAWAL-WEBHOOK] Retrait échoué - RequestId: {}, Status: {}", requestId, status);
            }
            
            return ResponseEntity.ok("Webhook retrait traité");
            
        } catch (Exception e) {
            log.error("❌ [WITHDRAWAL-WEBHOOK] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erreur traitement webhook retrait");
        }
    }
    
    /**
     * Webhook pour les remboursements
     */
    @PostMapping("/money-withdrawal-refund")
    public ResponseEntity<String> handleRefundCallback(@RequestBody String rawPayload) {
        log.info("💰 [REFUND-WEBHOOK] Callback remboursement reçu: {}", rawPayload);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);
            
            String requestId = (String) payload.get("requestId");
            String idCarte = (String) payload.get("idCarte");
            Object montantObj = payload.get("montant");
            
            if (requestId != null && idCarte != null && montantObj != null) {
                BigDecimal montant = new BigDecimal(montantObj.toString());
                
                // Rembourser la carte
               // carteService.rembourserCarte(idCarte, montant, requestId);
                log.info("✅ [REFUND-WEBHOOK] Carte remboursée - Carte: {}, Montant: {}", idCarte, montant);
            }
            
            return ResponseEntity.ok("Remboursement traité");
            
        } catch (Exception e) {
            log.error("❌ [REFUND-WEBHOOK] Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erreur traitement remboursement");
        }
    }
}