package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.dto.commission.HostDebtPaymentRequest;
import com.sportsbooking.mainapi.jpa.entity.HostDebtPayment;
import com.sportsbooking.mainapi.jpa.entity.HostDebts;
import com.sportsbooking.mainapi.jpa.entity.VenueEntity;
import com.sportsbooking.mainapi.jpa.entity.enums.HostDebtStatus;
import com.sportsbooking.mainapi.jpa.entity.enums.VenueStatus;
import com.sportsbooking.mainapi.jpa.repository.HostDebtPaymentRepository;
import com.sportsbooking.mainapi.jpa.repository.HostDebtsRepository;
import com.sportsbooking.mainapi.jpa.repository.VenueRepository;
import com.sportsbooking.mainapi.service.CommissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HostDebtPaymentServiceImplTest {

    @Mock
    private HostDebtPaymentRepository hostDebtPaymentRepository;

    @Mock
    private HostDebtsRepository hostDebtsRepository;

    @Mock
    private CommissionService commissionService;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private HostDebtPaymentServiceImpl hostDebtPaymentService;

    @Test
    void registerPayment_shouldMarkDebtsAsPaidAndEnableVenue() {
        UUID venueId = UUID.randomUUID();
        UUID debtId = UUID.randomUUID();

        HostDebtPaymentRequest request = new HostDebtPaymentRequest(
                debtId,
                venueId,
                new BigDecimal("150.00"),
                "DNI",
                "12345678"
        );

        HostDebtPayment mockSavedPayment = HostDebtPayment.builder()
                .id(UUID.randomUUID())
                .paidAmount(request.paidAmount())
                .tipoDoc(request.tipoDoc())
                .nroDocumento(request.nroDocumento())
                .status(2)
                .build();

        when(hostDebtPaymentRepository.save(any(HostDebtPayment.class))).thenReturn(mockSavedPayment);

        HostDebts overdueDebt = HostDebts.builder()
                .id(debtId)
                .venueId(venueId)
                .commissionAmount(new BigDecimal("150.00"))
                .status(HostDebtStatus.OVERDUE) // status 3
                .build();

        when(hostDebtsRepository.findByVenueIdAndStatusNot(venueId, HostDebtStatus.PAID))
                .thenReturn(List.of(overdueDebt));

        VenueEntity inactiveVenue = VenueEntity.builder()
                .id(venueId)
                .name("Salón Los Pinos")
                .status(VenueStatus.INACTIVE) // inhabilitado (0)
                .disabledReason(1) // inhabilitado automáticamente por deuda
                .build();

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(inactiveVenue));

        // Act
        HostDebtPayment result = hostDebtPaymentService.registerPayment(request);

        // Assert
        assertNotNull(result);
        assertEquals(mockSavedPayment.getId(), result.getId());

        // Verificamos que la deuda pasó a PAID (status 2)
        assertEquals(HostDebtStatus.PAID, overdueDebt.getStatus());
        assertEquals(new BigDecimal("150.00"), overdueDebt.getPaidAmount());
        assertEquals(mockSavedPayment.getId(), overdueDebt.getIdCommission());
        verify(hostDebtsRepository).save(overdueDebt);

        // Verificamos que el salón (venue) fue habilitado automáticamente
        assertEquals(VenueStatus.ACTIVE, inactiveVenue.getStatus());
        assertEquals(0, inactiveVenue.getDisabledReason());
        verify(venueRepository).saveAndFlush(inactiveVenue);

        // Verificamos el recálculo
        verify(commissionService).calculateCurrentCycleCommission(venueId);
    }
}

