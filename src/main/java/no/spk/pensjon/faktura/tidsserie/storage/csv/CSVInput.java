package no.spk.pensjon.faktura.tidsserie.storage.csv;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import no.spk.pensjon.faktura.tidsserie.batch.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;

/**
 * Parameterobjekt som helde p� kjennskapen til kvar datafilene som skal matast inn i tidsserien ligg tilgjengelig.
 * <br>
 * Det f�lest ogs� naturlig � plassere kunnskapen om kva oversetter-implementasjonar som skal benyttast for � behandle
 * datafilene her.
 * <br>
 * TODO: Testing/integrasjonstesting av den her
 * TODO: Robustifisere, skal vi/kor mange linjer feil skal vi tolerere ved innlesing og konvertering her?
 * Skal ei linje feil la heile batchk�yringa feile? Skal 1000 linjer gjere det? 1 million linjer? Somewhere in between?
 *
 * @author Tarjei Skorgenes
 */
public class CSVInput implements GrunnlagsdataRepository {
    private final List<CsvOversetter<? extends Tidsperiode<?>>> oversettere = new ArrayList<>();

    private final Charset dataencoding = Charset.forName("CP1252");

    private final Path directory;

    public CSVInput(final Path directory) {
        this.directory = directory;

        oversettere.add(new StatligLoennstrinnperiodeOversetter());
        oversettere.add(new ApotekLoennstrinnperiodeOversetter());
        oversettere.add(new OmregningsperiodeOversetter());
        oversettere.add(new AvtaleversjonOversetter());
        oversettere.add(new AvtaleproduktOversetter());
    }

    /**
     * Legger til <code>oversetter</code> som en av oversettarane som blir fors�kt brukt ved konvertering av linjer
     * fr� referansedata-filer til tidsperioder.
     *
     * @param oversetter ein ny oversetter
     * @return <code>this</code>
     */
    CSVInput addOversettere(final CsvOversetter<?> oversetter) {
        this.oversettere.add(oversetter);
        return this;
    }

    @Override
    public Stream<String> medlemsdata() {
        return readLinesFrom(medlemsdataFil());
    }

    @Override
    public Stream<Tidsperiode<?>> referansedata() {
        try {
            return referansedataFiler()
                    .flatMap(this::readLinesFrom)
                    .map(line -> line.split(";"))
                    .map(Arrays::asList)
                    .flatMap(this::oversettLinje);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Stream<Path> referansedataFiler() throws IOException {
        return Files
                .list(directory)
                .filter(path -> path.toString().endsWith("csv.gz"))
                .filter(path -> !path.toString().endsWith("medlemsdata.csv.gz"));
    }

    private Path medlemsdataFil() {
        return Paths.get(directory.toString(), "medlemsdata.csv.gz");
    }

    private Stream<? extends Tidsperiode<?>> oversettLinje(final List<String> linje) {
        // Kan ikkje inlinast i 1.8.0_11 p� grunn av type-inference feil ved kompilering
        final Function<CsvOversetter<? extends Tidsperiode<?>>, ? extends Tidsperiode<?>> mapper =
                oversetter -> oversetter.oversett(linje);
        return oversettere
                .stream()
                .filter(oversetter -> oversetter.supports(linje))
                        // TODO: Burde vi ha en reduce som verifiserer at det kun er ein oversetter som st�ttar linja?
                .map(mapper);
    }

    private Stream<String> readLinesFrom(final Path fil) {
        try {
            final BufferedReader reader = openReader(fil);
            return reader
                    .lines()
                    .onClose(closeOnCompletion(reader))
                    .filter(this::erGrunnlagsdatalinje);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean erGrunnlagsdatalinje(String line) {
        return !erKommentarlinje(line);
    }

    private boolean erKommentarlinje(String line) {
        return line.startsWith("#");
    }

    private BufferedReader openReader(final Path path) throws IOException {
        return new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(
                                                path.toFile()
                                        )
                                )
                        ),
                        dataencoding
                )
        );
    }

    private Runnable closeOnCompletion(final BufferedReader reader) {
        return () -> {
            try {
                reader.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
