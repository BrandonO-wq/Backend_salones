package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.dto.common.MessageResponse;
import com.sportsbooking.mainapi.dto.user.request.UpdateProfileRequest;
import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import com.sportsbooking.mainapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Implementación de {@link UserService}.
 *
 * <p>
 * Gestiona la edición del perfil del usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // ─────────────────────────────────────────────
    // Update Profile
    // ─────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>
     * Actualización parcial: solo modifica los campos que el cliente envía (no
     * nulos ni vacíos).
     */
    @Override
    @Transactional
    public MessageResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = findUserOrThrow(userId);

        if (StringUtils.hasText(request.name())) {
            user.setName(request.name().strip());
        }
        if (StringUtils.hasText(request.phone())) {
            user.setPhone(request.phone().strip());
        }
        if (StringUtils.hasText(request.ubigeo())) {
            user.setUbigeo(request.ubigeo().strip());
        }
        userRepository.save(user);

        log.info("Perfil actualizado para usuario ID: {}", userId);
        return new MessageResponse("Perfil actualizado exitosamente.");
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
    }
}
