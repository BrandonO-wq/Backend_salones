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
        HostDebts hostDebt;
        if (request.hostDebtId() != null) {
            hostDebt = hostDebtsRepository.findById(request.hostDebtId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada", request.hostDebtId()));
        } else if (request.venueId() != null) {
            // Calcular o asegurar el registro de deuda del ciclo activo para el venueId
            VenueCommissionResponse commResponse = commissionService.calculateCurrentCycleCommission(request.venueId());
            hostDebt = hostDebtsRepository.findById(commResponse.getHostDebtId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada para el venue", request.venueId()));
        } else {
            throw new BusinessException("Debe proporcionar el ID de la deuda (hostDebtId) o el ID del establecimiento (venueId).");
        }

        // 1. Crear el registro del pago
        HostDebtPayment payment = HostDebtPayment.builder()
                .hostDebtId(hostDebt.getId())
                .paidAmount(request.paidAmount())
                .tipoDoc(request.tipoDoc())
                .nroDocumento(request.nroDocumento())
                .status(2) // Default status (2 = PAID automáticamente)
                .build();

        // 2. Actualizar el pago acumulado de la deuda y cambiar estado a PAID (2)
        java.math.BigDecimal currentPaid = hostDebt.getPaidAmount() != null ? hostDebt.getPaidAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal newPaidAmount = currentPaid.add(request.paidAmount());
        hostDebt.setPaidAmount(newPaidAmount);
        if (newPaidAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            hostDebt.setStatus(com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus.PAID);
        }
        hostDebtsRepository.save(hostDebt);

        // 3. Recalcular la comisión del ciclo actual para actualizar prórroga de deadline y estado
        commissionService.calculateCurrentCycleCommission(hostDebt.getVenueId());

        // 4. Guardar y retornar el pago
        return hostDebtPaymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostDebtPaymentResponse> getPayments(UUID hostUserId) {
        return hostDebtPaymentRepository.findPaymentsByHostUserId(hostUserId);
    }
}
