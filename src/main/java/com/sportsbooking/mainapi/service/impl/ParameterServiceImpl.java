package com.sportsbooking.mainapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbooking.mainapi.config.RedisProperties;
import com.sportsbooking.mainapi.dto.parameter.ParameterDto;
import com.sportsbooking.mainapi.jpa.myrepository.ParameterMyRepository;
import com.sportsbooking.mainapi.service.ParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Implementación de {@link ParameterService} con cache Redis (TTL 12h).
 *
 * <p>Key en Redis: {@code sportsbooking:params:parent:{id}}.
 * Si la key existe, retorna los datos desde Redis; si no, consulta la BD
 * mediante MyBatis y almacena el resultado serializado como JSON.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterServiceImpl implements ParameterService {

    private final ParameterMyRepository parameterMyRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;
    private final ObjectMapper objectMapper;

    /** TTL de 12 horas para el cache de parámetros. */
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    /**
     * Obtiene parámetros hijos activos de una categoría padre.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Intenta leer desde Redis.</li>
     *   <li>Si no existe, consulta la BD via MyBatis.</li>
     *   <li>Serializa y almacena en Redis con TTL 12h.</li>
     * </ol>
     *
     * @param parentId ID del parámetro padre.
     * @return lista de parámetros hijos activos.
     */
    @Override
    public List<ParameterDto> getByParent(int parentId) {
        String key = redisProperties.getKeyPrefix() + "param_v5:parent:" + parentId;

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("[Parameter] Cache hit para parent={}", parentId);
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("[Parameter] Error leyendo cache Redis para parent={}: {}", parentId, e.getMessage());
        }

        List<ParameterDto> params = parameterMyRepository.getChildrenByParent(parentId);

        try {
            String json = objectMapper.writeValueAsString(params);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.info("[Parameter] Cache miss → almacenados {} params para parent={}", params.size(), parentId);
        } catch (Exception e) {
            log.warn("[Parameter] Error escribiendo cache Redis para parent={}: {}", parentId, e.getMessage());
        }

        return params;
    }
}
