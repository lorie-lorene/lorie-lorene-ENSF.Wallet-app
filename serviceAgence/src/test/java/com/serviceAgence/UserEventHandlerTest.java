package com.serviceAgence;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.serviceAgence.dto.RegistrationProcessingResult;
import com.serviceAgence.event.PasswordResetRequestEvent;
import com.serviceAgence.event.UserRegistrationEventReceived;
import com.serviceAgence.messaging.AgenceEventPublisher;
import com.serviceAgence.messaging.UserEventHandler;
import com.serviceAgence.services.AgenceService;


@ExtendWith(MockitoExtension.class)
class UserEventHandlerTest {

    @Mock
    private AgenceService agenceService;

    @Mock
    private AgenceEventPublisher eventPublisher;

    @InjectMocks
    private UserEventHandler userEventHandler;

    private UserRegistrationEventReceived validRegistrationEvent;
    private PasswordResetRequestEvent validPasswordResetEvent;

    @BeforeEach
    void setUp() {
        // Event d'inscription valide
        validRegistrationEvent = new UserRegistrationEventReceived();
        validRegistrationEvent.setEventId("EVENT_123");
        validRegistrationEvent.setIdClient("CLIENT123");
        validRegistrationEvent.setIdAgence("AGENCE001");
        validRegistrationEvent.setCni("123456789");
        validRegistrationEvent.setEmail("test@example.com");
        validRegistrationEvent.setNom("DUPONT");
        validRegistrationEvent.setPrenom("Jean");
        validRegistrationEvent.setNumero("655123456");
        
        // Images encodées en Base64
        byte[] testImage = {(byte) 0xFF, (byte) 0xD8, 0x00, 0x00}; // Header JPEG
        validRegistrationEvent.setRectoCni(Base64.getEncoder().encodeToString(testImage));
        validRegistrationEvent.setVersoCni(Base64.getEncoder().encodeToString(testImage));
        validRegistrationEvent.setSourceService("UserService");

        // Event de reset password
        validPasswordResetEvent = new PasswordResetRequestEvent();
        validPasswordResetEvent.setEventId("RESET_123");
        validPasswordResetEvent.setIdClient("CLIENT123");
        validPasswordResetEvent.setCni("123456789");
        validPasswordResetEvent.setEmail("test@example.com");
        validPasswordResetEvent.setNumero("655123456");
        validPasswordResetEvent.setNom("DUPONT");
    }

    @Test
    void testHandleUserRegistration_Success() {
        // Given
        RegistrationProcessingResult successResult = RegistrationProcessingResult.accepted(
            123456789L, "Compte créé avec succès");
        
        when(agenceService.processRegistrationRequest(any()))
            .thenReturn(successResult);

        // When
        assertDoesNotThrow(() -> userEventHandler.handleUserRegistration(validRegistrationEvent));

        // Then
        verify(agenceService).processRegistrationRequest(any());
        verify(eventPublisher).sendRegistrationResponse(
            eq("CLIENT123"), eq("AGENCE001"), eq("test@example.com"), eq(successResult));
    }

    @Test
    void testHandleUserRegistration_Rejected() {
        // Given
        RegistrationProcessingResult rejectedResult = RegistrationProcessingResult.rejected(
            "KYC_FAILED", "Documents non conformes");
        
        when(agenceService.processRegistrationRequest(any()))
            .thenReturn(rejectedResult);

        // When
        assertDoesNotThrow(() -> userEventHandler.handleUserRegistration(validRegistrationEvent));

        // Then
        verify(agenceService).processRegistrationRequest(any());
        verify(eventPublisher).sendRegistrationResponse(
            eq("CLIENT123"), eq("AGENCE001"), eq("test@example.com"), eq(rejectedResult));
    }

    @Test
    void testHandleUserRegistration_TechnicalError() {
        // Given
        when(agenceService.processRegistrationRequest(any()))
            .thenThrow(new RuntimeException("Erreur technique"));

        // When
        assertDoesNotThrow(() -> userEventHandler.handleUserRegistration(validRegistrationEvent));

        // Then
        verify(agenceService).processRegistrationRequest(any());
        verify(eventPublisher).sendRegistrationResponse(
            eq("CLIENT123"), eq("AGENCE001"), eq("test@example.com"), any(RegistrationProcessingResult.class));
    }

    @Test
    void testHandlePasswordResetRequest_Success() {
        // When
        assertDoesNotThrow(() -> userEventHandler.handlePasswordResetRequest(validPasswordResetEvent));

        // Then
        verify(eventPublisher).sendPasswordResetResponse(
            eq("123456789"), anyString(), eq("test@example.com"), eq("AGENCE_SYSTEM"));
    }

    @Test
    void testConvertToRegistrationRequest_WithValidBase64() {
        // When
        assertDoesNotThrow(() -> userEventHandler.handleUserRegistration(validRegistrationEvent));

        // Then - Vérifier que la conversion s'est bien passée
        verify(agenceService).processRegistrationRequest(argThat(request ->
            request.getIdClient().equals("CLIENT123") &&
            request.getCni().equals("123456789") &&
            request.getEmail().equals("test@example.com") &&
            request.getRectoCni() != null &&
            request.getVersoCni() != null
        ));
    }

    @Test
    void testConvertToRegistrationRequest_WithInvalidBase64() {
        // Given
        validRegistrationEvent.setRectoCni("INVALID_BASE64!!!");
        validRegistrationEvent.setVersoCni("ANOTHER_INVALID_BASE64");

        // When
        assertDoesNotThrow(() -> userEventHandler.handleUserRegistration(validRegistrationEvent));

        // Then - Doit continuer malgré l'erreur de décodage
        verify(agenceService).processRegistrationRequest(argThat(request ->
            request.getIdClient().equals("CLIENT123") &&
            request.getRectoCni() == null && // Images nulles à cause du décodage raté
            request.getVersoCni() == null
        ));
    }

    @Test
    void testGenerateTemporaryPassword() {
        // When - Tester indirectement via handlePasswordResetRequest
        assertDoesNotThrow(() -> userEventHandler.handlePasswordResetRequest(validPasswordResetEvent));

        // Then - Vérifier qu'un mot de passe a été généré
        verify(eventPublisher).sendPasswordResetResponse(
            eq("123456789"), 
            argThat(password -> password != null && password.startsWith("TEMP") && password.length() >= 12), 
            eq("test@example.com"), 
            eq("AGENCE_SYSTEM")
        );
    }
}
