package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentRequest;
import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentResponse;
import com.sportsbooking.mainapi.jpa.entity.HostDebtPayment;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.repository.HostDebtPaymentRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.service.HostDebtPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HostDebtPaymentServiceImpl implements HostDebtPaymentService {

    private final HostDebtPaymentRepository hostDebtPaymentRepository;
    private final HostDebtsRepository hostDebtsRepository;

    @Override
    @Transactional
    public HostDebtPayment registerPayment(HostDebtPaymentRequest request) {
        // 1. Verify that the Host Debt exists
        HostDebts hostDebt = hostDebtsRepository.findById(request.hostDebtId())
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada", request.hostDebtId()));

        // 2. Create the payment record
        HostDebtPayment payment = HostDebtPayment.builder()
                .hostDebtId(request.hostDebtId())
                .paidAmount(request.paidAmount())
                .tipoDoc(request.tipoDoc())
                .nroDocumento(request.nroDocumento())
                .status(2) // Default status (2 = PAID automatically)
                .build();

        // 3. Update the associated host debt status to PAID and save it
        hostDebt.setStatus(com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus.PAID);
        hostDebt.setPaidAmount(request.paidAmount());
        hostDebtsRepository.save(hostDebt);

        // 4. Save the payment
        return hostDebtPaymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostDebtPaymentResponse> getPayments(UUID hostUserId) {
        return hostDebtPaymentRepository.findPaymentsByHostUserId(hostUserId);
    }
}
