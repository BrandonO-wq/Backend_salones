package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.commission.HostDebtorResponseDto;
import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.dto.commission.RegisteredHostResponseDto;
import com.sportsbooking.mainapi.jpa.entity.HostDebtResponseDto;

import java.util.List;
import java.util.UUID;

public interface CommissionService {
    List<HostDebtResponseDto> getHostDebts(UUID hostId);

    VenueCommissionResponse calculateCurrentCycleCommission(UUID venueId);

    List<HostDebtorResponseDto> getHostDebtors();

    List<RegisteredHostResponseDto> getRegisteredHosts();
}
