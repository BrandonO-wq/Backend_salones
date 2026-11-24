package com.sportsbooking.mainapi.dto.venue.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueDetailedReportDto {
    private String ownerName;             // el cliente (nombre del dueño/anfitrión)
    private String ownerEmail;            // el correo (correo del dueño/anfitrión)
    private OffsetDateTime ownerCreatedAt; // fecha de creacion (fecha de creacion del dueño/anfitrión)
    
    private UUID venueId;                 // ID del salon
    private String venueName;             // Nombre del salon
    private OffsetDateTime venueCreatedAt; // fecha de creacion de su salon
    private String ubigeo;                // su ubigeo (código)
    private String ubigeoText;            // su ubigeo (descripción, e.g. Lima, Miraflores)
    private String operatingHours;        // el horario de atencion (del salon, e.g. "08:00 - 22:00")
    
    private Long bookingsCount;           // cantidad de reservas (ingresos en cantidad)
    private BigDecimal totalIncome;       // cantidad de ingresos hasta la fecha (ingresos en monto)
    private BigDecimal totalCommissions;  // las comisiones
    private BigDecimal totalEarnings;     // las ganancias del salon
    
    private LocalDate latestBookingDate;  // fecha de reserva (fecha de la reserva más reciente, si existe)
    private List<BookingDetailReportDto> bookings; // listado de reservas detalladas

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingDetailReportDto {
        private UUID bookingId;
        private LocalDate bookingDate;     // fecha de reserva
        private OffsetDateTime createdAt;  // fecha de creación de la reserva
        private String bookingHours;       // horario de atencion de la reserva (ej: "10:00 - 12:00")
        private BigDecimal totalAmount;    // monto de la reserva
        private BigDecimal commission;     // comisión de la reserva
        private BigDecimal earnings;       // ganancia del salón por la reserva
        private Integer status;            // estado de la reserva
        private String clientName;         // nombre del cliente que reservó
        private String clientEmail;        // correo del cliente que reservó
    }
}
