package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad que representa una billetera digital asociada a un venue.
 *
 * <p>Mapeada a la tabla {@code venue_pay_wallet}.
 * El campo {@code idDigitalWallet} referencia la tabla {@code parameter} (categoría Billeteras).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venue_pay_wallet")
public class VenuePayWalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    /** ID de la billetera digital (referencia a parameter). */
    @Column(name = "id_digital_wallet", nullable = false)
    private Integer idDigitalWallet;

    @Column(name = "nombre_titular", nullable = false)
    private String nombreTitular;

    @Column(name = "numero_celular", nullable = false)
    private String numeroCelular;
}
