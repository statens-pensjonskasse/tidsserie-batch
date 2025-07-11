package no.spk.tidsserie.batch.core.grunnlagsdata.csv;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static no.spk.tidsserie.batch.core.grunnlagsdata.csv.DuplisertCSVFilException.sjekkForDuplikat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import no.spk.tidsserie.batch.core.grunnlagsdata.GrunnlagsdataRepository;
import no.spk.tidsserie.tidsperiode.Tidsperiode;

/**
 * {@link CSVInput} støttar deserialisering av {@link #referansedata() tidsperioder} og
 * {@link #medlemsdata() medlemsdata} frå flate filer på CSV-format.
 * <br>
 * Tenesta er parametriserbar via {@link #addOversettere(CsvOversetter)} / {@link #addOversettere(Stream)}, sjølve
 * deserialiseringa blir plugga inn i form av {@link CsvOversetter oversettere} som støttar ei bestemt type CSV-rad,
 * typisk basert på ein eller anna form for type-indikator i første kolonne på kvar rad.
 * <br>
 * Tenesta forventar å bli satt opp med ein innkatalog som inneheld GZIP-komprimerte CSV-filer som kategoriserast i ein
 * av to kategoriar:
 * <ul>
 * <li>{@link #referansedata() referansedata / tidsperioder}</li>
 * <li>{@link #medlemsdata() medlemsdata}</li>
 * </ul>
 * <br>
 * Medlemsdata kan lesast frå fila medlemsdata.csv.gz i tenestas innkatalog. Det er støtte for at medlemsdata.csv.gz er
 * partisjonert på fleire filer så lenge dei startar med medlemsdata. Når ein jobbar med partisjonerte medlemsdata vil ein
 * kunne gruppere desse ved å leggje dei i ein eigjen katalog med namnet medlemsdata i tenestas innkatalog.
 * Alle andre filer av type csv.gz forventast å tilhøyre {@link #referansedata() referansedata}-kategorien og blir
 * forventa å inneholde {@link Tidsperiode tidsperioder} av forskjelliger typer.
 */
public class CSVInput implements GrunnlagsdataRepository {
    private static final int DO_NOT_STRIP_TRAILING_SEPARATORS = -1;

    private final List<CsvOversetter<? extends Tidsperiode<?>>> oversettere = new ArrayList<>();

    private final Charset dataencoding = Charset.forName("UTF-8");

    private final Path directory;

    /**
     * Konstruerer ei ny teneste som forventar å finne referanse- og medlemsdata i csv.gz-filer lagra direkte under den
     * angitte katalogen.
     *
     * @param innkatalog innkatalogen tenesta skal lese csv.gz-filer frå
     * @throws NullPointerException dersom <code>innkatalog</code> er <code>null</code>
     */
    public CSVInput(final Path innkatalog) {
        this.directory = requireNonNull(innkatalog, "innkatalog er påkrevd, men var null");
    }

    /**
     * Legger til ein <code>oversetter</code> som blir forsøkt brukt ved konvertering av linjer frå referansedata-filer
     * til tidsperioder.
     *
     * @param oversetter ein oversetter som tenesta skal kunne benytte seg av
     * @return <code>this</code>
     * @see #referansedata()
     */
    public CSVInput addOversettere(final CsvOversetter<?> oversetter) {
        this.oversettere.add(oversetter);
        return this;
    }

    /**
     * Legger til eit sett med <code>oversettere</code> som blir forsøkt brukt ved konvertering av linjer frå
     * referansedata-filer til tidsperioder.
     *
     * @param oversettere ein samling oversettere som tenesta skal kunne benytte seg av
     * @return <code>this</code>
     * @see #referansedata()
     */
    public CSVInput addOversettere(final Stream<CsvOversetter<?>> oversettere) {
        oversettere.forEach(this::addOversettere);
        return this;
    }

    @Override
    public Stream<List<String>> medlemsdata() {
        return medlemsdataFiler()
                .flatMap(this::readLinesFrom);
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
     * Returnerer stien til alle komprimerte CSV-filer som inneheld referansedata som ikkje er medlemsspesifikke.
     * <br>
     * Referansedatafiler blir plukka basert på at dei har filending <code>csv.gz</code> og ikkje har filnavn
     * medlemsdata.csv.gz.
     * <br>
     *
     * @return ein straum med stien til alle referansedatafiler generert av faktura-grunnlagsdata-batch
     * @throws IOException dersom ein uvent I/O-feil oppstår under utlisting av filene
     */
    Stream<Path> referansedataFiler() throws IOException {
        try (final Stream<Path> filer = Files
                .list(directory)
                .filter(path -> path.toString().endsWith("csv.gz") || path.toString().endsWith(".csv"))
                .filter(path -> !path.toFile().getName().startsWith("medlemsdata.csv"))
                .peek(DuplisertCSVFilException::sjekkForDuplikat)) {
            return filer.toList().stream();
        }
    }

    private Stream<Path> medlemsdataFiler() {
        return Stream.concat(
                        Arrays.stream(requireNonNull(directory.toFile().listFiles(), format("Inputmappe %s finnes ikke", directory))),
                        medlemsdataFilerFraMedlemsdatamappe().stream()
                )
                .filter(CSVInput::erMedlemsdataFil)
                .map(File::toPath)
                .peek(DuplisertCSVFilException::sjekkForDuplikat);
    }

    private List<File> medlemsdataFilerFraMedlemsdatamappe() {
        if (Files.exists(directory.resolve("medlemsdata"))) {
            try (Stream<Path> paths = Files.walk(directory.resolve("medlemsdata"))) {
                return paths.map(Path::toFile).toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return emptyList();
        }
    }

    private static boolean erMedlemsdataFil(final File fil) {
        final Pattern regex = Pattern.compile("^medlemsdata(\\d+(-.+)?)?\\.csv(\\.gz)?$");
        return regex.matcher(fil.getName()).find();
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
                        open(path),
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

    private static InputStream open(final Path path) throws IOException {
        final File file = sjekkForDuplikat(path).toFile();
        final BufferedInputStream input = new BufferedInputStream(
                new FileInputStream(file)
        );
        if (file.getName().endsWith(".gz")) {
            return new GZIPInputStream(input);
        }
        return input;
    }
}
