package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {
    boolean existsByBookingCode(String bookingCode);

    @Query("SELECT b FROM BookingEntity b JOIN FETCH b.details WHERE b.venueId = :venueId AND b.bookingDate = :bookingDate AND b.status IN :statuses")
    List<BookingEntity> findByVenueIdAndBookingDateAndStatusInWithDetails(
            @Param("venueId") UUID venueId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("statuses") List<Integer> statuses);

    List<BookingEntity> findByClientUserIdOrderByCreatedAtDesc(UUID clientUserId);

    @Query("SELECT DISTINCT b, v FROM BookingEntity b JOIN VenueEntity v ON b.venueId = v.id LEFT JOIN FETCH b.details WHERE b.clientUserId = :clientUserId ORDER BY b.bookingDate ASC, b.createdAt ASC")
    List<Object[]> findBookingsWithVenueByClientUserId(@Param("clientUserId") UUID clientUserId);

    List<BookingEntity> findByVenueIdOrderByCreatedAtDesc(UUID venueId);

    @Query("SELECT COUNT(b) FROM BookingEntity b, VenueEntity v WHERE b.venueId = v.id AND v.hostUserId = :hostUserId AND b.status = :status")
    long countPendingByHostUserId(@Param("hostUserId") UUID hostUserId, @Param("status") int status);

    @Query("SELECT DISTINCT b FROM BookingEntity b JOIN VenueEntity v ON b.venueId = v.id LEFT JOIN FETCH b.details WHERE v.hostUserId = :hostUserId ORDER BY b.createdAt ASC")
    List<BookingEntity> findBookingsByHostUserId(@Param("hostUserId") UUID hostUserId);

    List<BookingEntity> findByStatus(int status);
}
