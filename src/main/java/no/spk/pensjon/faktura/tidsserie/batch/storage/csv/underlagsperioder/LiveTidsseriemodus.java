package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.stream.Collectors.joining;

import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * {@link LiveTidsseriemodus} setter opp batchen til å generere ein live-tidsserie på periodenivå, formatert i
 * henhold til kontrakta for innmating til Qlikview og EDW/DVH.
 * <br>
 * Inntil vidare brukar live-tidsserien avregningsreglane sidan prognosereglane ikkje inkluderer støtte for beregning
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

    private final Regelsett reglar = new AvregningsRegelsett();

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
     * @param facade     fasada som blir brukt for å generere tidsserien
     * @param publikator backend-systemet som observasjonane av kvar periode blir lagra via
     * @return ein by publikator som serialiserer og lagrar alle underlagsperioder for kvart observasjonsunderlag i
     * tidsserien som blir generert av <code>facade</code>
     * @see Datavarehusformat#serialiser(Underlag, Underlagsperiode)
     * @see StorageBackend#lagre(Consumer)
     */
    @Override
    public Observasjonspublikator create(final TidsserieFacade facade, final StorageBackend publikator) {
        return s -> {
            s.forEach(observasjonsunderlag -> {
                observasjonsunderlag
                        .stream()
                        .map(underlagsperiode -> outputFormat.serialiser(
                                observasjonsunderlag,
                                underlagsperiode
                        ))
                        .map(kolonneVerdiar -> kolonneVerdiar.map(Object::toString).collect(joining(";")))
                        .forEach(line -> publikator.lagre(builder -> builder.append(line).append('\n')));
            });
        };
    }
}
