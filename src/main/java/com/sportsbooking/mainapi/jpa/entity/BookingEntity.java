package com.sportsbooking.mainapi.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "client_user_id", nullable = false)
    private UUID clientUserId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "total_hours", nullable = false)
    private Integer totalHours;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private int status = 1;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "sports_id", columnDefinition = "integer[]", nullable = false)
    @Builder.Default
    private Integer[] sportsId = {};

    @Column(name = "base_price_day", precision = 10, scale = 2)
    private BigDecimal basePriceDay;

    @Column(name = "base_price_night", precision = 10, scale = 2)
    private BigDecimal basePriceNight;

    @Column(name = "id_payment_method")
    private Integer idPaymentMethod;

    @Column(name = "booking_code", nullable = false)
    private String bookingCode;

    @Column(name = "voucher_url")
    private String voucherUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "is_self_host")
    @Builder.Default
    private Boolean isSelfHost = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingDetailEntity> details = new ArrayList<>();

    // helper function to maintain bi-directional relationship
    public void addDetail(BookingDetailEntity detail) {
        details.add(detail);
        detail.setBooking(this);
    }
}
