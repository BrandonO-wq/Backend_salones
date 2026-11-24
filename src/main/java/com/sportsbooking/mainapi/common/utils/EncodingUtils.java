package com.sportsbooking.mainapi.common.utils;

public final class EncodingUtils {

    private EncodingUtils() {
    }

    /**
     * Sanitiza y corrige texto que pueda contener mojibake producto de una mala interpretación
     * de bytes UTF-8 como ISO-8859-1 o Windows-1252.
     *
     * Ejemplos:
     * - "âš ï¸" -> "⚠️"
     * - "dÃas" / "dÃ­as" -> "días"
     *
     * @param input Texto original.
     * @return Texto corregido en UTF-8 limpio.
     */
    public static String sanitizeMojibake(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String cleaned = input;

        // 1. Corrección de emojis conocidos mal decodificados (especialmente el warning emoji)
        cleaned = cleaned.replace("âš ï¸", "⚠️")
                         .replace("âš\uFE0F", "⚠️")
                         .replace("âš\u00A0ï¸\u008F", "⚠️")
                         .replace("âš\u00A0ï¸", "⚠️")
                         .replace("âš ", "⚠️ ")
                         .replace("âš", "⚠️");

        // 2. Corrección de palabras y caracteres en español con tildes/eñes mal decodificados
        // Caso específico "dÃas" (cuando se omite o pierde el byte secundario \u00ad)
        cleaned = cleaned.replace("dÃas", "días")
                         .replace("DÃas", "Días")
                         .replace("aÃ±os", "años")
                         .replace("AÃ±os", "Años");

        // Reemplazos de mojibake estándar UTF-8 -> ISO-8859-1 / Windows-1252
        if (cleaned.contains("Ã") || cleaned.contains("Â")) {
            cleaned = cleaned
                    .replace("Ã¡", "á")
                    .replace("Ã©", "é")
                    .replace("Ã\u00AD", "í")
                    .replace("Ã­", "í")
                    .replace("Ã³", "ó")
                    .replace("Ãº", "ú")
                    .replace("Ã±", "ñ")
                    .replace("Ã\u0081", "Á")
                    .replace("Ã\u0089", "É")
                    .replace("Ã\u008D", "Í")
                    .replace("Ã\u0093", "Ó")
                    .replace("Ã\u009A", "Ú")
                    .replace("Ã\u0091", "Ñ")
                    .replace("Â¿", "¿")
                    .replace("Â¡", "¡");
        }

        return cleaned;
    }
}

