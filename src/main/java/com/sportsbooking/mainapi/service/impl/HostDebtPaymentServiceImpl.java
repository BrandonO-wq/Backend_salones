package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.common.exception.BusinessException;
import com.sportsbooking.mainapi.common.exception.ResourceNotFoundException;
import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentRequest;
import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentResponse;
import com.sportsbooking.mainapi.dto.commission.VenueCommissionResponse;
import com.sportsbooking.mainapi.jpa.entity.HostDebtPayment;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.repository.HostDebtPaymentRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.service.CommissionService;
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
    private final CommissionService commissionService;

    @Override
    @Transactional
    public HostDebtPayment registerPayment(HostDebtPaymentRequest request) {
        // 1. Registrar el pago en commission_payments para generar el ID
        HostDebtPayment payment = HostDebtPayment.builder()
                .paidAmount(request.paidAmount())
                .tipoDoc(request.tipoDoc())
                .nroDocumento(request.nroDocumento())
                .status(2) // 2 = PAID automáticamente
                .build();
        payment = hostDebtPaymentRepository.save(payment);

        UUID venueId = request.venueId();
        if (venueId == null && request.hostDebtId() != null) {
            HostDebts debt = hostDebtsRepository.findById(request.hostDebtId()).orElse(null);
            if (debt != null) {
                venueId = debt.getVenueId();
            }
        }

        if (venueId == null) {
            throw new BusinessException("Debe proporcionar el ID del establecimiento (venueId) o un ID de deuda válido.");
        }

        // 2. Obtener todas las filas de host_debts con status = PENDING (1) para este venue
        List<HostDebts> pendingDebts = hostDebtsRepository.findByVenueIdAndStatus(venueId, com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus.PENDING);

        // 3. Asignar el ID de la comisión generada y pasar el status a PAID (2) de cada fila
        for (HostDebts debt : pendingDebts) {
            debt.setIdCommission(payment.getId());
            debt.setStatus(com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus.PAID);
            if (debt.getCommissionAmount() != null) {
                debt.setPaidAmount(debt.getCommissionAmount());
            }
            hostDebtsRepository.save(debt);
        }

        // 4. Recalcular la comisión del ciclo actual para actualizar estados del venue
        commissionService.calculateCurrentCycleCommission(venueId);

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostDebtPaymentResponse> getPayments(UUID hostUserId) {
        return hostDebtPaymentRepository.findPaymentsByHostUserId(hostUserId);
    }
}
