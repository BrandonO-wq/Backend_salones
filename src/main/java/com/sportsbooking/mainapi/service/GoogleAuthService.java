package com.sportsbooking.mainapi.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Servicio encargado de validar ID Tokens de Google.
 */
@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Valida un ID Token de Google y retorna el payload con la información del usuario.
     *
     * @param idTokenString ID Token recibido del frontend.
     * @return payload del token verificado.
     * @throws IllegalArgumentException si el token es inválido o ocurre algún error en la validación.
     */
    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
            throw new IllegalArgumentException("El token de Google es inválido o ha expirado.");
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("Error al validar el token de Google: " + e.getMessage());
        }
    }
}
