package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.commission.HostDebtorResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.dto.commission.RegisteredHostResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueDebtReportResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;

import com.sportsbooking.mainapi.dto.commission.ConfirmCycleConfigChangeRequest;
import com.sportsbooking.mainapi.dto.common.MessageResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CommissionService {
    List<HostDebtResponseDto> getHostDebts(UUID hostId);

    com.sportsbooking.mainapi.jpa.entity.HostDebts registerHostDebtForBooking(com.sportsbooking.mainapi.jpa.entity.BookingEntity booking);

    VenueCommissionResponse calculateCurrentCycleCommission(UUID venueId);

    List<HostDebtorResponseDto> getHostDebtors();

    List<RegisteredHostResponseDto> getRegisteredHosts();

    MessageResponse requestCommissionChange(UUID adminUserId);

    MessageResponse confirmCommissionChange(UUID adminUserId, String otpCode, Double newValue);

    Double getCommissionPercentage();

    List<VenueDebtReportResponseDto> getVenueDebtReport();

    MessageResponse toggleVenueStatus(UUID venueId, boolean active);

    MessageResponse enableVenue(UUID venueId);

    Map<String, Object> getVenueDisabledStatus(UUID venueId);

    Map<String, Object> getCycleConfig();

    MessageResponse requestCycleConfigChange(UUID adminUserId);

    MessageResponse confirmCycleConfigChange(UUID adminUserId, ConfirmCycleConfigChangeRequest request);
}
