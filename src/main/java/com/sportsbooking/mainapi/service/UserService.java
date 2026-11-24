package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.user.request.UpdateProfileRequest;

import java.util.UUID;

/**
 * Contrato de negocio para operaciones sobre el perfil del usuario.
 *
 * <p>Expone:
 * <ul>
 *   <li>{@link #updateProfile} – actualiza nombre y teléfono del usuario.</li>
 * </ul>
 */
public interface UserService {

    /**
     * Actualiza el nombre completo y el teléfono del usuario autenticado.
     *
     * @param userId  ID del usuario obtenido del JWT.
     * @param request nuevos valores de nombre y teléfono.
     * @return mensaje de confirmación.
     */
    MessageResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
