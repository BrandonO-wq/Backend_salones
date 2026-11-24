package com.sportsbooking.mainapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuración de Firebase Admin SDK.
 *
 * <p>Inicializa {@link FirebaseApp} una sola vez al arrancar el contexto de Spring,
 * usando las credenciales de servicio ubicadas en {@code classpath:firebase-service-account.json}.
 *
 * <p>La ruta del archivo es configurable via {@code firebase.service-account-path} en
 * {@code application.yml}, lo que permite usar rutas distintas por entorno.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path:firebase-service-account.json}")
    private String serviceAccountPath;

    /**
     * Inicializa {@link FirebaseApp} al arrancar el contexto.
     *
     * <p>Si ya existe una instancia de {@link FirebaseApp} (ej: en tests), no se vuelve
     * a inicializar para evitar {@link IllegalStateException}.
     *
     * @throws IOException si el archivo de credenciales no se encuentra en el classpath.
     */
    @PostConstruct
    public void initFirebase() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[Firebase] FirebaseApp ya inicializado — saltando re-inicialización.");
            return;
        }

        try (InputStream serviceAccount =
                     new ClassPathResource(serviceAccountPath).getInputStream()) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("[Firebase] FirebaseApp inicializado correctamente.");
        } catch (IOException e) {
            log.error("[Firebase] No se encontró el archivo de credenciales: '{}'." +
                    " Verifica que existe en src/main/resources.", serviceAccountPath);
            throw e;
        }
    }
}
