package com.sportsbooking.mainapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbooking.mainapi.config.RedisProperties;
import com.sportsbooking.mainapi.dto.ubigeo.UbigeoPeruRaw;
import com.sportsbooking.mainapi.dto.ubigeo.UbigeoTreeResponse;
import com.sportsbooking.mainapi.dto.ubigeo.UbigeoTreeResponse.*;
import com.sportsbooking.mainapi.jpa.myrepository.UbigeoMyRepository;
import com.sportsbooking.mainapi.service.UbigeoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación de {@link UbigeoService} con cache Redis (TTL 24h).
 *
 * <p>Carga los ubigeos desde la tabla ubigeo_peru, los convierte en un árbol
 * jerárquico y lo cachea en Redis.
 *
 * <p>Key en Redis: {@code sportsbooking:ubigeos:tree}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UbigeoServiceImpl implements UbigeoService {

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;
    private final ObjectMapper objectMapper;
    private final UbigeoMyRepository ubigeoMyRepository;

    /** TTL de 24 horas para el cache de ubigeos. */
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /** Árbol preconstruido en memoria como fallback si Redis falla. */
    private UbigeoTreeResponse cachedTree;

    /**
     * Precarga el árbol de ubigeos al iniciar la aplicación.
     * Lo guarda en Redis y en memoria local como fallback.
     */
    @PostConstruct
    void init() {
        try {
            cachedTree = buildTree();
            String key = redisProperties.getKeyPrefix() + "ubigeos:tree";
            String json = objectMapper.writeValueAsString(cachedTree);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.info("[Ubigeo] Árbol cargado: {} departamentos desde PostgreSQL", cachedTree.departamentos().size());
        } catch (Exception e) {
            log.error("[Ubigeo] Error cargando árbol de ubigeos desde PostgreSQL: {}", e.getMessage(), e);
        }
    }

    @Override
    public UbigeoTreeResponse getTree() {
        String key = redisProperties.getKeyPrefix() + "ubigeos:tree";

        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, UbigeoTreeResponse.class);
            }
        } catch (Exception e) {
            log.warn("[Ubigeo] Error leyendo Redis, usando cache local o DB: {}", e.getMessage());
        }

        // Si Redis falla o expiró, reconstruir de PostgreSQL y re-cachear
        if (cachedTree == null) {
            cachedTree = buildTree();
        }

        try {
            String json = objectMapper.writeValueAsString(cachedTree);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("[Ubigeo] Error re-cacheando en Redis: {}", e.getMessage());
        }

        return cachedTree;
    }

    @Override
    public String getUbigeoText(String ubigeoId) {
        if (ubigeoId == null || ubigeoId.length() != 6) return ubigeoId;
        String text = ubigeoMyRepository.getUbigeoText(ubigeoId);
        return text != null ? text : ubigeoId;
    }

    // ─────────────────────────────────────────────
    // Construcción del árbol desde PostgreSQL
    // ─────────────────────────────────────────────

    /**
     * Extrae data serializada de la Base de Datos y la convierte en el modelo DTO esperado
     */
    private UbigeoTreeResponse buildTree() {
        List<UbigeoPeruRaw> rows = ubigeoMyRepository.findAll();

        Map<String, String> deptNames = new LinkedHashMap<>();
        Map<String, String> provNames = new LinkedHashMap<>();
        Map<String, List<Distrito>> distsByProv = new LinkedHashMap<>();

        for (UbigeoPeruRaw row : rows) {
            if (row.getInei() == null || row.getInei().length() < 6) continue;
            String dCode = row.getInei().substring(0, 2);
            String pCode = row.getInei().substring(0, 4);

            deptNames.putIfAbsent(dCode, row.getDepartamento());
            provNames.putIfAbsent(pCode, row.getProvincia());

            distsByProv.computeIfAbsent(pCode, k -> new ArrayList<>())
                    .add(new Distrito(row.getInei(), row.getDistrito(), row.getLatitude(), row.getLongitude()));
        }

        List<Departamento> departamentos = deptNames.entrySet().stream()
                .map(dEntry -> {
                    String dCode = dEntry.getKey();
                    List<Provincia> provincias = provNames.entrySet().stream()
                            .filter(pEntry -> pEntry.getKey().startsWith(dCode))
                            .map(pEntry -> {
                                String pCode = pEntry.getKey();
                                List<Distrito> distritos = distsByProv.getOrDefault(pCode, Collections.emptyList());
                                return new Provincia(pCode, pEntry.getValue(), distritos);
                            })
                            .toList();
                    return new Departamento(dCode, dEntry.getValue(), provincias);
                })
                .toList();

        return new UbigeoTreeResponse(departamentos);
    }
}
