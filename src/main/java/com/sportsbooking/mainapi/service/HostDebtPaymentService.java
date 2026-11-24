package com.sportsbooking.mainapi.service;

import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentRequest;
import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentResponse;
import com.sportsbooking.mainapi.jpa.entity.HostDebtPayment;

import java.util.List;
import java.util.UUID;

public interface HostDebtPaymentService {
    HostDebtPayment registerPayment(HostDebtPaymentRequest request);
    List<HostDebtPaymentResponse> getPayments(UUID hostUserId);
}
