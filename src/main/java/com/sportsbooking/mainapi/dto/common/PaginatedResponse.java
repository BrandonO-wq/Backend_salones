package com.sportsbooking.mainapi.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO genérico para retornos paginados.
 * Evita la serialización directa de la clase PageImpl interna de Spring Data,
 * lo cual produce warnings en logs y acopla la estructura del frontend al framework.
 *
 * @param <T> Tipo del contenido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;

    /**
     * Mapea de manera limpia un Page de Spring a nuestro propio DTO para la respuesta HTTP.
     */
    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .number(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
