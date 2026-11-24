package com.sportsbooking.mainapi.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncodingUtilsTest {

    @Test
    void testSanitizeMojibake_WarningEmojiAndDias() {
        String input = "âš ï¸ Tienes deudas pendientes de hace 3 dÃas";
        String expected = "⚠️ Tienes deudas pendientes de hace 3 días";

        assertEquals(expected, EncodingUtils.sanitizeMojibake(input));
    }

    @Test
    void testSanitizeMojibake_AccentedVowels() {
        String input = "NotificaciÃ³n: Recordatorio de confirmaciÃ³n";
        String expected = "Notificación: Recordatorio de confirmación";

        assertEquals(expected, EncodingUtils.sanitizeMojibake(input));
    }

    @Test
    void testSanitizeMojibake_NullOrCleanInput() {
        assertNull(EncodingUtils.sanitizeMojibake(null));
        assertEquals("", EncodingUtils.sanitizeMojibake(""));
        assertEquals("⚠️ Mensaje limpio en español con tildes: días",
                EncodingUtils.sanitizeMojibake("⚠️ Mensaje limpio en español con tildes: días"));
    }
}

