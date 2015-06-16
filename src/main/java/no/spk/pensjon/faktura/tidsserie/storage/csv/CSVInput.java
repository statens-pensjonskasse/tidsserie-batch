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
 * Parameterobjekt som helde på kjennskapen til kvar datafilene som skal matast inn i tidsserien ligg tilgjengelig.
 * <br>
 * Det følest også naturlig å plassere kunnskapen om kva oversetter-implementasjonar som skal benyttast for å behandle
 * datafilene her.
 * <br>
 * TODO: Testing/integrasjonstesting av den her
 * TODO: Robustifisere, skal vi/kor mange linjer feil skal vi tolerere ved innlesing og konvertering her?
 * Skal ei linje feil la heile batchkøyringa feile? Skal 1000 linjer gjere det? 1 million linjer? Somewhere in between?
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

    @Override
    public Stream<String> medlemsdata() throws IOException {
        return openReader(medlemsdataFil())
                .lines()
                .filter(this::erGrunnlagsdatalinje)
                ;
    }

    @Override
    public Stream<Tidsperiode<?>> referansedata() throws IOException {
        return referansedataFiler()
                .flatMap(path -> {
                    try {
                        // TODO: Verifisere at/om readerane her faktisk blir lukka automatisk når streamen blir closa
                        // TODO: Når blir streamen egentli closa, skjer det når kvar terminal-operation blir fullført?
                        final BufferedReader reader = openReader(path);
                        return reader.lines();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .filter(this::erGrunnlagsdatalinje)
                .map(line -> line.split(";"))
                .map(Arrays::asList)
                .flatMap(this::oversettLinje);
    }

    private Path medlemsdataFil() {
        return Paths.get(directory.toString(), "medlemsdata.csv.gz");
    }

    private Stream<Path> referansedataFiler() throws IOException {
        return Files
                .list(directory)
                .filter(path -> path.toString().endsWith("csv.gz"))
                .filter(path -> !path.toString().startsWith("medlemsdata"))
                .filter(path -> !path.toString().startsWith("underlagsperiode"));
    }

    private Stream<? extends Tidsperiode<?>> oversettLinje(final List<String> linje) {
        // Kan ikkje inlinast i 1.8.0_11 på grunn av type-inference feil ved kompilering
        final Function<CsvOversetter<? extends Tidsperiode<?>>, ? extends Tidsperiode<?>> mapper =
                oversetter -> oversetter.oversett(linje);
        return oversettere
                .stream()
                .filter(oversetter -> oversetter.supports(linje))
                        // TODO: Burde vi ha en reduce som verifiserer at det kun er ein oversetter som støttar linja?
                .map(mapper);
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
}
