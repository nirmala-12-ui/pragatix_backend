package jjcet.PragatiX.integrations.neopat.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility for ensuring all timestamps in Neopat SMS integration
 * are strictly generated, parsed, and stored in Indian Standard Time (IST - Asia/Kolkata).
 */
public final class NeopatDateTimeUtil {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    private static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    };

    private NeopatDateTimeUtil() {
    }

    /**
     * Returns the current LocalDateTime in Asia/Kolkata (IST).
     */
    public static LocalDateTime nowIst() {
        return LocalDateTime.now(IST_ZONE);
    }

    /**
     * Parses an incoming date/time string (such as Neopat UTC starttime / submittime)
     * and converts it accurately to IST (Asia/Kolkata).
     */
    public static LocalDateTime parseAndConvertToIst(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String clean = text.trim();

        // 1. Try parsing with explicit offset/zone (e.g. 2026-10-09T03:30:00Z)
        try {
            Instant instant = Instant.parse(clean);
            return instant.atZone(IST_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(clean);
            return odt.atZoneSameInstant(IST_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(clean);
            return zdt.withZoneSameInstant(IST_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }

        // 2. Parse without zone offset. Per Neopat contract, starttime and submittime are UTC.
        // Convert that UTC instant to Asia/Kolkata (IST).
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(clean, formatter);
                return ldt.atZone(UTC_ZONE).withZoneSameInstant(IST_ZONE).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    /**
     * Parses an incoming date/time string from Neopat and preserves it in UTC as sent.
     */
    public static LocalDateTime parseAsUtc(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String clean = text.trim();

        try {
            Instant instant = Instant.parse(clean);
            return LocalDateTime.ofInstant(instant, UTC_ZONE);
        } catch (Exception ignored) {
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(clean);
            return odt.atZoneSameInstant(UTC_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(clean);
            return zdt.withZoneSameInstant(UTC_ZONE).toLocalDateTime();
        } catch (Exception ignored) {
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(clean, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }
}
