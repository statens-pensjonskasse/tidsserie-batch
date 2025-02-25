package no.spk.tidsserie.batch.core;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static no.spk.tidsserie.batch.core.BatchIdConstants.GRUNNLAGSDATA_PATTERN;
import static no.spk.tidsserie.batch.core.BatchIdConstants.GRUNNLAGSDATA_PREFIX;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link UttrekksId} representerer navnet på eit uttrekk som batchen kan bruke til å lese inn grunnlagsdata frå.
 * <p>
 * Navnet på eit uttrekk tilsvarar navnet på ein underkatalog under {@link Katalog#GRUNNLAGSDATA inn-katalogen}
 * til batchen. Alt av CSV-filer som batchen skal kunne lese grunnlagsdata frå forventast å ligge lagra
 * i underkatalogen {@link UttrekksId} inneheld navnet på.
 *
 * @since 1.1.0
 */
public class UttrekksId {
    private static final Comparator<UttrekksId> SORTER_ETTER_ALDER_NYESTE_SIST = comparing(a -> a.verdi);
    private static final String PATTERN_TIDSPUINKT = "yyyy-MM-dd_HH-mm-ss-SS";
    private static final DateTimeFormatter FORMAT_TIDSPUNKT = ofPattern(PATTERN_TIDSPUINKT);

    private final String verdi;

    private UttrekksId(final String verdi) {
        this.verdi = requireNonNull(verdi, "verdi er påkrevd, men var null");
    }

    /**
     * Konverterer <code>verdi</code> til ein ny {@link UttrekksId}.
     * <p>
     * Det blir ikkje utført noko form for semantisk validering av om uttrekket <code>verdi</code> peikar til, faktisk
     * er eit gyldig uttrekk. Det er kun syntaktisk validering av om navnet er på rett format som blir utført.
     *
     * @param verdi navnet på eit uttrekk
     * @return eit identifikator for uttrekket
     * @throws IllegalArgumentException dersom <code>verdi</code> ikkje er syntaktisk korrekt
     */
    public static UttrekksId uttrekksId(final String verdi) {
        return parse(verdi).orElseThrow(
                () -> new IllegalArgumentException(
                        format(
                                "Verdien '%s' er ikke en gyldig identifikator for et uttrekk.\nKoden må være på formatet %s%s",
                                verdi,
                                GRUNNLAGSDATA_PREFIX,
                                PATTERN_TIDSPUINKT
                        )
                )
        );
    }

    /**
     * Velger det nyeste uttrekket fra <code>kandidater</code>.
     * <p>
     * For at en kandidat skal kunne bli valgt må den matche følgende forventninger:
     * <ol>
     * <li>Den må være en {@link File#isDirectory() katalog}</li>
     * <li>Den må ha et navn som starter på {@value BatchIdConstants#GRUNNLAGSDATA_PREFIX}</li>
     * <li>Den må ha et navn som slutter med et tidspunkt på formatet {@value #PATTERN_TIDSPUINKT}</li>
     * </ol>
     *
     * @param kandidater kandidatene som det nyeste uttrekk kan velges fra
     * @return det nyeste uttrekket blant kandidatene, eller {@link Optional#empty() ingenting} dersom ingen av kandidatene matcher kravene til et uttrekk
     */
    public static Optional<UttrekksId> velgNyeste(final Stream<Path> kandidater) {
        return kandidater
                .filter(f -> f.toFile().isDirectory())
                .map(f -> f.toFile().getName())
                .map(UttrekksId::parse)
                .flatMap(UttrekksId::asStream)
                .max(UttrekksId.SORTER_ETTER_ALDER_NYESTE_SIST);
    }

    /**
     * Returnerer den fulle stien til katalogen som uttrekket identifiserer.
     *
     * @param innkatalog innkatalogen som underkatalogen for uttrekket ligger lagret under
     * @return stien til uttrekkskatalogen
     */
    public Path resolve(final Path innkatalog) {
        return innkatalog
                .resolve(verdi)
                .toAbsolutePath();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }

        final UttrekksId that = (UttrekksId) o;
        return this.verdi.equals(that.verdi);
    }

    @Override
    public int hashCode() {
        return verdi.hashCode();
    }

    @Override
    public String toString() {
        return verdi;
    }

    private static Optional<UttrekksId> parse(final String verdi) {
        return Optional.ofNullable(verdi)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(UttrekksId::erGyldig)
                .map(UttrekksId::new);
    }

    private static boolean erGyldig(final String value) {
        if (!GRUNNLAGSDATA_PATTERN.matcher(value).matches()) {
            return false;
        }
        final String tidspunkt = value.substring(GRUNNLAGSDATA_PREFIX.length());
        try {
            LocalDateTime.parse(tidspunkt, FORMAT_TIDSPUNKT);
        } catch (final DateTimeParseException e) {
            return false;
        }
        return true;
    }

    private static Stream<UttrekksId> asStream(final Optional<UttrekksId> uttrekk) {
        return uttrekk
                .map(Stream::of)
                .orElseGet(Stream::empty);
    }
}