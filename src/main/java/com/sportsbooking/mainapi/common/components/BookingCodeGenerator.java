package com.sportsbooking.mainapi.common.components;

import com.sportsbooking.mainapi.jpa.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BookingCodeGenerator {

    private final BookingRepository bookingRepository;

    @Value("${app.booking.random-code-length:5}")
    private int randomCodeLength;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    public String generateUniqueCode() {
        int currentYear = LocalDate.now().getYear();
        String generatedCode;
        do {
            StringBuilder sb = new StringBuilder(randomCodeLength);
            for (int i = 0; i < randomCodeLength; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            generatedCode = String.format("RES-%d-%s", currentYear, sb.toString());
        } while (bookingRepository.existsByBookingCode(generatedCode));
        
        return generatedCode;
    }
}
