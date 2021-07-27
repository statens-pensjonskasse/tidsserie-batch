package no.spk.felles.tidsserie.batch.main.input;

import static java.time.temporal.TemporalQueries.localDate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class Datoar {
    private static final DateTimeFormatter yyyyMMddFormatUtenPunktum = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter yyyyMMddFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private Datoar() {
    }

    public static LocalDate dato(String text) {
        if (text == null) {
            return null;
        } else {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            } else {
                switch (trimmed.length()) {
                    case 8:
                        return yyyyMMddFormatUtenPunktum.parse(trimmed).query(localDate());
                    case 10:
                        return yyyyMMddFormat.parse(trimmed).query(localDate());
                    default:
                        throw new IllegalArgumentException(
                                "Teksten \'"
                                        + trimmed
                                        + "\' inneheld ikkje ein gyldig dato, "
                                        + "det er kun datoar på formata yyyy.MM.dd / yyyyMMdd som er støtta."
                        );
                }
            }
        }
    }
}