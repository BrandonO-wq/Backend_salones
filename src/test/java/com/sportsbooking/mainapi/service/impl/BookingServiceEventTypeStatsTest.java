package com.sportsbooking.mainapi.service.impl;

import com.sportsbooking.mainapi.dto.booking.response.EventTypeBookingCountDto;
import com.sportsbooking.mainapi.dto.parameter.ParameterDto;
import com.sportsbooking.mainapi.jpa.entity.BookingEntity;
import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import com.sportsbooking.mainapi.service.ParameterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceEventTypeStatsTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParameterService parameterService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private List<ParameterDto> mockParams;

    @BeforeEach
    void setUp() {
        ParameterDto p1 = new ParameterDto();
        p1.setIdParam(1);
        p1.setChildName("Boda");

        ParameterDto p2 = new ParameterDto();
        p2.setIdParam(2);
        p2.setChildName("Cumpleaños");

        ParameterDto p3 = new ParameterDto();
        p3.setIdParam(3);
        p3.setChildName("Graduación");

        mockParams = List.of(p1, p2, p3);
    }

    @Test
    void testCountBookingsByEventType_multipleEventsInSingleBooking() {
        // Reserva 1: eventos [1, 2]
        BookingEntity b1 = BookingEntity.builder()
                .id(UUID.randomUUID())
                .sportsId(new Integer[]{1, 2})
                .bookingDate(LocalDate.now())
                .status(1)
                .build();

        // Reserva 2: evento [1]
        BookingEntity b2 = BookingEntity.builder()
                .id(UUID.randomUUID())
                .sportsId(new Integer[]{1})
                .bookingDate(LocalDate.now())
                .status(1)
                .build();

        // Reserva 3: eventos [2, 3, 2] -> contiene duplicado dentro del array
        BookingEntity b3 = BookingEntity.builder()
                .id(UUID.randomUUID())
                .sportsId(new Integer[]{2, 3, 2})
                .bookingDate(LocalDate.now())
                .status(1)
                .build();

        when(parameterService.getByParent(1)).thenReturn(mockParams);
        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        List<EventTypeBookingCountDto> result = bookingService.getBookingsCountByEventType(null, null, null, null);

        assertNotNull(result);
        assertEquals(3, result.size());

        // Evento 1 ("Boda"): Reserva 1 + Reserva 2 = 2 reservas
        EventTypeBookingCountDto dto1 = result.stream().filter(r -> r.getEventTypeId().equals(1)).findFirst().orElseThrow();
        assertEquals(2L, dto1.getBookingCount());
        assertEquals("Boda", dto1.getEventTypeName());

        // Evento 2 ("Cumpleaños"): Reserva 1 + Reserva 3 = 2 reservas
        EventTypeBookingCountDto dto2 = result.stream().filter(r -> r.getEventTypeId().equals(2)).findFirst().orElseThrow();
        assertEquals(2L, dto2.getBookingCount());
        assertEquals("Cumpleaños", dto2.getEventTypeName());

        // Evento 3 ("Graduación"): Reserva 3 = 1 reserva
        EventTypeBookingCountDto dto3 = result.stream().filter(r -> r.getEventTypeId().equals(3)).findFirst().orElseThrow();
        assertEquals(1L, dto3.getBookingCount());
        assertEquals("Graduación", dto3.getEventTypeName());
    }

    @Test
    void testCountBookingsByEventType_eventTypeWithZeroBookings() {
        BookingEntity b1 = BookingEntity.builder()
                .id(UUID.randomUUID())
                .sportsId(new Integer[]{1})
                .bookingDate(LocalDate.now())
                .status(1)
                .build();

        when(parameterService.getByParent(1)).thenReturn(mockParams);
        when(bookingRepository.findAll()).thenReturn(List.of(b1));

        List<EventTypeBookingCountDto> result = bookingService.getBookingsCountByEventType(null, null, null, null);

        assertNotNull(result);
        assertEquals(3, result.size());

        EventTypeBookingCountDto dto3 = result.stream().filter(r -> r.getEventTypeId().equals(3)).findFirst().orElseThrow();
        assertEquals(0L, dto3.getBookingCount());
        assertEquals(0.0, dto3.getPercentage());
    }

    @Test
    void testGetAdminEventTypesCount_returnsMapOfEventTypesAndTotals() {
        BookingEntity b1 = BookingEntity.builder()
                .id(UUID.randomUUID())
                .sportsId(new Integer[]{1, 2})
                .status(1)
                .build();

        BookingEntity b2 = BookingEntity.builder()
                .id(UUID.randomUUID())
                .sportsId(new Integer[]{2})
                .status(1)
                .build();

        when(parameterService.getByParent(1)).thenReturn(mockParams);
        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2));

        java.util.Map<String, Long> adminCounts = bookingService.getAdminEventTypesCount();

        assertNotNull(adminCounts);
        // Cumpleaños: 2 (b1 y b2)
        assertEquals(2L, adminCounts.get("Cumpleaños"));
        // Boda: 1 (b1)
        assertEquals(1L, adminCounts.get("Boda"));
        // Graduación: 0
        assertEquals(0L, adminCounts.get("Graduación"));
    }
}

