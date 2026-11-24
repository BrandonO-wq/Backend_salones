package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.ubigeo.UbigeoTreeResponse;

/**
 * Servicio para consulta de ubigeos del Perú.
 *
 * <p>Los datos se cargan desde JSON estáticos ({@code resources/data/})
 * y se cachean en Redis con TTL de 24 horas.
 */
public interface UbigeoService {

    /**
     * Retorna el árbol completo de ubigeos: Departamento → Provincia → Distrito.
     *
     * @return estructura jerárquica con todos los ubigeos de Perú.
     */
    UbigeoTreeResponse getTree();

    /**
     * Resuelve un código ubigeo crudo (Ej. "150132")
     * devolviendo el formato amigable: "Provincia, Distrito"
     */
    String getUbigeoText(String ubigeoId);
}
