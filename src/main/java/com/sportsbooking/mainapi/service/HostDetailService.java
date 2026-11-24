package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.user.response.HostDetailResponseDto;
import java.util.UUID;

public interface HostDetailService {
    HostDetailResponseDto getHostDetail(UUID hostId);
}
