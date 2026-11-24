package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad que representa una cuenta bancaria asociada a un venue.
 *
 * <p>Mapeada a la tabla {@code venue_pay_bank}.
 * El campo {@code idBank} referencia la tabla {@code parameter} (categoría Bancos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venue_pay_bank")
public class VenuePayBankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    /** ID del banco (referencia a parameter). */
    @Column(name = "id_bank", nullable = false)
    private Integer idBank;

    @Column(name = "nombre_titular", nullable = false)
    private String nombreTitular;

    @Column(name = "numero_cuenta", nullable = false)
    private String numeroCuenta;

    @Column(name = "cci")
    private String cci;
}
