package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.joining;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.TidsserieResulat;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.PrognoseRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link LiveTidsseriemodus} setter opp batchen til � generere ein live-tidsserie p� periodeniv�, formatert i
 * henhold til kontrakta for innmating til Qlikview og EDW/DVH.
 * <br>
 * Inntil vidare brukar live-tidsserien avregningsreglane sidan prognosereglane ikkje inkluderer st�tte for beregning
 * av  YSK/GRU-faktureringsandel.
 * <br>
 * Ansvaret ofr generering av verdiane som endar opp i CSV-formatet for DVH-innmatinga, blir handtert av
 * {@link Datavarehusformat}.
 *
 * @author Tarjei Skorgenes
 * @see Datavarehusformat
 */
public class LiveTidsseriemodus implements Tidsseriemodus {
    private final CSVFormat outputFormat = new Datavarehusformat();

    private final Regelsett reglar = new PrognoseRegelsett();

    private final Tidsserienummer nummer = Tidsserienummer.genererForDato(now());

    /**
     * Kolonnenavna CSV-formatet for live-tidsserien benyttar seg av.
     *
     * @return ein straum med alle kolonnenavna for live-tidsserien
     * @see Datavarehusformat#kolonnenavn()
     */
    @Override
    public Stream<String> kolonnenavn() {
        return outputFormat.kolonnenavn();
    }

    /**
     * Regelsettet som live-tidsserien skal benytte.
     *
     * @return regelsettet som skal benyttast ved oppbygging av live-tidsserien
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
     * @param facade      fasada som blir brukt for � generere tidsserien
     * @param serienummer serienummer som alle eventar som blir sendt vidare til <code>backend</code> for persistering
     *                    skal tilh�yre
     * @param publikator  backend-systemet som observasjonane av kvar periode blir lagra via
     * @return ein by publikator som serialiserer og lagrar alle underlagsperioder for kvart observasjonsunderlag i
     * tidsserien som blir generert av <code>facade</code>
     * @see Datavarehusformat#serialiser(Underlag, Underlagsperiode)
     * @see StorageBackend#lagre(Consumer)
     */
    @Override
    public Observasjonspublikator create(final TidsserieFacade facade, long serienummer, final StorageBackend publikator) {
        return nyPublikator(
                this::serialiserPeriode,
                line -> lagre(publikator, line, serienummer)
        );
    }

    /**
     * Genererer ein ny publikator som ved hjelp av <code>mapper </code> mappar alle observasjonsunderlagas perioder om
     * til ei form som deretter blir lagra via <code>lagring</code>.
     * <br>
     * Kvart observasjonsunderlag blir annotert med {@link no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer}
     * basert p� dagens dato slik at mapperen unikt kan indikere at alle periodene tilh�yrer ein og samme tidsserie.
     *
     * @param mapper  serialiserer observasjonsunderlagas underlagsperioder til formatet <code>lagring</code> skal lagre p�
     * @param lagring tar den serialiserte versjonen av underlagsperiodene og lagrar dei
     * @param <T>     datatypen underlagsperiodene blir serialisert til
     * @return ein ny observasjonspublikator som kan brukast til � serialisere og lagre innholdet fr� ein tidsserie
     */
    <T> Observasjonspublikator nyPublikator(
            final Function<Underlag, Stream<T>> mapper, final Consumer<T> lagring) {
        return s -> s
                .peek(this::annoterMedTidsserienummer)
                .flatMap(mapper)
                .forEach(lagring);
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

    private void lagre(final StorageBackend publikator, final String line, final long serienummer) {
        publikator.lagre(event -> event.serienummer(serienummer).buffer.append(line).append("\n"));
    }

    @Override
    public void completed(TidsserieResulat tidsserieResulat) {
        new LiveTidsserieAvslutter(tidsserieResulat)
                .lagCsvGruppefiler()
                .lagTriggerfil();
    }

}
