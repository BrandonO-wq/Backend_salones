package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.dto.user.response.HostDetailResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.entity.UserEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.jpa.repository.UserRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueReviewRepository;
import com.sportsbooking.mainapi.service.HostDetailService;
import com.sportsbooking.mainapi.service.UbigeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HostDetailServiceImpl implements HostDetailService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;
    private final VenueReviewRepository venueReviewRepository;
    private final HostDebtsRepository hostDebtsRepository;
    private final UbigeoService ubigeoService;

    @Override
    @Transactional(readOnly = true)
    public HostDetailResponseDto getHostDetail(UUID hostId) {
        UserEntity user = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", hostId));

        long venuesCount = venueRepository.countByHostUserId(hostId);
        long bookingsCount = bookingRepository.countBookingsByHostUserId(hostId);
        double averageRating = venueReviewRepository.avgPointsByHostUserId(hostId);
        BigDecimal totalEarnings = bookingRepository.sumEarningsByHostUserId(hostId);

        List<HostDebts> unpaidDebts = hostDebtsRepository.findByHostUserIdAndStatusNot(hostId, HostDebtStatus.PAID);

        BigDecimal debtAmount = BigDecimal.ZERO;
        OffsetDateTime deadline = null;
        long delayDays = 0;

        if (!unpaidDebts.isEmpty()) {
            // Total unpaid debt: sum of (commissionAmount - paidAmount)
            debtAmount = unpaidDebts.stream()
                    .map(debt -> {
                        BigDecimal commission = debt.getCommissionAmount() != null ? debt.getCommissionAmount() : BigDecimal.ZERO;
                        BigDecimal paid = debt.getPaidAmount() != null ? debt.getPaidAmount() : BigDecimal.ZERO;
                        return commission.subtract(paid);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Oldest deadline among unpaid debts
            deadline = unpaidDebts.stream()
                    .map(HostDebts::getDeadline)
                    .filter(dl -> dl != null)
                    .min(OffsetDateTime::compareTo)
                    .orElse(null);

            // Max delay days
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            delayDays = unpaidDebts.stream()
                    .mapToLong(debt -> {
                        if (debt.getDeadline() != null && debt.getDeadline().isBefore(now)) {
                            return ChronoUnit.DAYS.between(debt.getDeadline(), now);
                        }
                        return 0L;
                    })
                    .max()
                    .orElse(0L);
        }

        String ubigeoText = ubigeoService.getUbigeoText(user.getUbigeo());

        return HostDetailResponseDto.builder()
                .hostId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .ubigeo(user.getUbigeo())
                .ubigeoText(ubigeoText)
                .createdAt(user.getCreatedAt())
                .venuesCount(venuesCount)
                .bookingsCount(bookingsCount)
                .averageRating(averageRating)
                .debtAmount(debtAmount)
                .totalEarnings(totalEarnings)
                .deadline(deadline)
                .delayDays(delayDays)
                .build();
    }
}
