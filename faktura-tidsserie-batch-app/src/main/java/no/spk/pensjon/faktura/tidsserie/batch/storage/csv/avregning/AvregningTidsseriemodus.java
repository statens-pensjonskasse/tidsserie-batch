package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.avregning;

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsavtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.medlemsdata.Medlemsdata;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.csv.AvregningsavtaleperiodeOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.AvregningsperiodeOversetter;
import no.spk.pensjon.faktura.tidsserie.storage.csv.CSVInput;

/**
 * {@link AvregningTidsseriemodus} kan benyttes for � generere en tidsserie tilrettelagt for avregning.
 * <br>
 * Modusen genererer tidsserier som lagres til flate filer p� CSV-format, i henhold til formatspesifikasjonen
 * definert via {@link Avregningformat}.
 * <br>
 * Forel�pig st�tter avregningsmodusen kun premieberegning for pensjonsproduktene, forsikringsproduktene avventer
 * avklaring fra forretning p� hvordan de skal avregnes.
 *
 * @author Tarjei Skorgenes
 * @see Avregningformat
 * @since 1.2.0
 */
public class AvregningTidsseriemodus implements Tidsseriemodus {
    private final CSVFormat outputFormat = new Avregningformat();

    private final Regelsett reglar = new AvregningsRegelsett();

    private final Tidsserienummer nummer = Tidsserienummer.genererForDato(now());

    private Optional<Set<AvtaleId>> avtaler = Optional.empty();

    @Override
    public Stream<Tidsperiode<?>> referansedata(final TidsperiodeFactory perioder) {
        return Stream.of(
                reglar.reglar(),
                perioder.loennsdata(),
                perioder.perioderAvType(Avregningsperiode.class),
                perioder.perioderAvType(Avregningsavtaleperiode.class)
        )
                .flatMap(s -> s);
    }

    @Override
    public GrunnlagsdataRepository repository(final Path directory) {
        validerFilFinnes(directory, "avregningsperioder.csv.gz");
        validerFilFinnes(directory, "avregningsavtaler.csv.gz");
        avtaler = Optional.of(new HashSet<>());
        return new CSVInput(directory)
                .addOversettere(new AvregningsperiodeOversetter())
                .addOversettere(new AvregningsavtaleperiodeOversetter(avtale -> avtaler.get().add(avtale)));
    }

    private void validerFilFinnes(Path directory, String file) {
        if (!directory.resolve(file).toFile().exists()) {
            throw new IllegalStateException(file + " finnes ikke i " + directory);
        }
    }

    /**
     * Navnet til alle kolonnene som CSV-filene kan inneholde verdier for.
     *
     * @return ein str�m med navn p� alle kolonnene i CSV-filene til tidsserien
     * @see Avregningformat#kolonnenavn()
     */
    @Override
    public Stream<String> kolonnenavn() {
        return outputFormat.kolonnenavn();
    }

    @Override
    public void partitionInitialized(long serienummer, StorageBackend storage) {
        //noop
    }

    @Override
    public void initStorage(final StorageBackend lager) {
        lager.lagre(event -> event.buffer
                .append(kolonnenavn().collect(joining(";")))
                .append('\n'));
    }

    /**
     * Beregningsreglene som benyttes ved generering av tidsserien.
     *
     * @return regelsettet som modusen benytter
     * @see AvregningsRegelsett
     */
    @Override
    public Regelsett regelsett() {
        return reglar;
    }

    /**
     * Genererer ein ny publikator som genererer og lagrar ein observasjon pr underlagsperiode pr
     * observasjonsunderlag.
     * <br>
     *
     * @param facade fasada som blir brukt for � generere tidsserien
     * @param serienummer serienummer som alle eventar som blir sendt vidare til <code>backend</code> for persistering
     * skal tilh�yre
     * @param publikator backend-systemet som observasjonane av kvar periode blir lagra via
     * @return ein by publikator som serialiserer og lagrar alle underlagsperioder for kvart observasjonsunderlag i
     * tidsserien som blir generert av <code>facade</code>
     * @see Avregningformat#serialiser(Underlag, Underlagsperiode)
     * @see StorageBackend#lagre(Consumer)
     */
    @Override
    public Observasjonspublikator create(final TidsserieFacade facade, long serienummer, final StorageBackend publikator) {
        return nyPublikator(
                this::serialiserPeriode,
                line -> lagre(publikator, line)
        );
    }

    /**
     * Genererer ein ny publikator som ved hjelp av <code>mapper </code> serialiserer alle periodene fr� alle
     * observasjonsunderlag, til ei form som deretter blir lagra via <code>lagring</code>.
     *
     * @param mapper serialiserer observasjonsunderlagas underlagsperioder til formatet <code>lagring</code> skal lagre p�
     * @param lagring tar den serialiserte versjonen av underlagsperiodene og lagrar dei
     * @param <T> datatypen underlagsperiodene blir serialisert til
     * @return ein ny observasjonspublikator som kan brukast til � serialisere og lagre innholdet fr� ein tidsserie
     */
    <T> Observasjonspublikator nyPublikator(
            final Function<Underlag, Stream<T>> mapper, final Consumer<T> lagring) {
        return s -> s
                .peek(this::annoterMedTidsserienummer)
                .filter(u -> u.annotasjonFor(Observasjonsdato.class).erAaretsSisteDag())
                .flatMap(mapper)
                .forEach(lagring);
    }

    void lagre(final StorageBackend publikator, final String line) {
        publikator.lagre(event -> event.buffer.append(line).append("\n"));
    }

    private Underlag annoterMedTidsserienummer(Underlag u) {
        return u.annoter(Tidsserienummer.class, nummer);
    }

    private Stream<String> serialiserPeriode(final Underlag observasjonsunderlag) {
        return observasjonsunderlag
                .stream()
                .map(underlagsperiode -> outputFormat.serialiser(
                        observasjonsunderlag,
                        underlagsperiode
                ))
                .map(kolonneVerdiar -> kolonneVerdiar.map(Object::toString).collect(joining(";")));
    }

    @Override
    public boolean behandleMedlem(Medlemsdata medlemsdata) {
        return medlemsdata
                .avtalekoblingar(
                        p -> avtaler
                                .orElseThrow(
                                        () -> new IllegalStateException("Referansedata m� v�re innlest f�r denne metoden kan benyttes")
                                )
                                .contains(p.avtale())
                )
                .findAny()
                .isPresent();
    }


}
