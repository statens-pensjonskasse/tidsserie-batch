package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.stream.Collectors.toList;

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
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.core.GrunnlagsdataRepository;

/**
 * Parameterobjekt som helde på kjennskapen til kvar datafilene som skal matast inn i tidsserien ligg tilgjengelig.
 * <br>
 * Det følest også naturlig å plassere kunnskapen om kva oversetter-implementasjonar som skal benyttast for å behandle
 * datafilene her.
 *
 * @author Tarjei Skorgenes
 */
public class CSVInput implements GrunnlagsdataRepository {
    private static final int DO_NOT_STRIP_TRAILING_SEPARATORS = -1;
    private final List<CsvOversetter<? extends Tidsperiode<?>>> oversettere = new ArrayList<>();

    private final Charset dataencoding = Charset.forName("UTF-8");

    private final Path directory;

    public CSVInput(final Path directory) {
        this.directory = directory;

        oversettere.add(new StatligLoennstrinnperiodeOversetter());
        oversettere.add(new ApotekLoennstrinnperiodeOversetter());
        oversettere.add(new OmregningsperiodeOversetter());
        oversettere.add(new AvtaleversjonOversetter());
        oversettere.add(new AvtaleproduktOversetter());
        oversettere.add(new AvtaleperiodeOversetter());
        oversettere.add(new ArbeidsgiverOversetter());
        oversettere.add(new ArbeidsgiverdataperiodeOversetter());
    }

    /**
     * Legger til <code>oversetter</code> som en av oversettarane som blir forsøkt brukt ved konvertering av linjer
     * frå referansedata-filer til tidsperioder.
     *
     * @param oversetter ein ny oversetter
     * @return <code>this</code>
     */
    public CSVInput addOversettere(final CsvOversetter<?> oversetter) {
        this.oversettere.add(oversetter);
        return this;
    }

    @Override
    public Stream<List<String>> medlemsdata() {
        return readLinesFrom(medlemsdataFil());
    }

    @Override
    public Stream<Tidsperiode<?>> referansedata() {
        try {
            return referansedataFiler()
                    .flatMap(this::readLinesFrom)
                    .flatMap(this::oversettLinje);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returnerer stien til alle CSV-filer som inneheld referansedata som ikkje er
     * medlemsspesifikke.
     * <br>
     * Referansedatafiler blir plukka basert på at dei har filending <code>csv.gz</code> og ikkje
     * har filnavn medlemsdata.csv.gz.
     * <br>
     *
     * @return ein straum med stien til alle referansedatafiler generert av faktura-grunnlagsdata-batch
     * @throws IOException dersom ein uvent I/O-feil oppstår under utlisting av filene
     */
    Stream<Path> referansedataFiler() throws IOException {
        try(final Stream<Path> filer = Files
                .list(directory)
                .filter(path -> path.toString().endsWith("csv.gz"))
                .filter(path -> !path.toString().endsWith("medlemsdata.csv.gz"))){
            return filer.collect(toList()).stream();
        }
    }

    private Path medlemsdataFil() {
        return Paths.get(directory.toString(), "medlemsdata.csv.gz");
    }

    private Stream<? extends Tidsperiode<?>> oversettLinje(final List<String> linje) {
        // Kan ikkje inlinast i 1.8.0_11 på grunn av type-inference feil ved kompilering
        final Function<CsvOversetter<? extends Tidsperiode<?>>, ? extends Tidsperiode<?>> mapper =
                oversetter -> oversetter.oversett(linje);
        return oversettere
                .stream()
                .filter(oversetter -> oversetter.supports(linje))
                .map(mapper);
    }

    private Stream<List<String>> readLinesFrom(final Path fil) {
        try {
            final BufferedReader reader = openReader(fil);
            return reader
                    .lines()
                    .onClose(closeOnCompletion(reader))
                    .filter(this::erGrunnlagsdatalinje)
                    .map(line -> line.split(";", DO_NOT_STRIP_TRAILING_SEPARATORS))
                    .map(Arrays::asList);
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
