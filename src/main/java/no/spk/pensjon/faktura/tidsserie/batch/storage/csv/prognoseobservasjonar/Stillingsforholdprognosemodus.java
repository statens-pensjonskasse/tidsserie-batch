package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.prognoseobservasjonar;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsseriemodus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aarsverk;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.PrognoseRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieFacade;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.TidsserieObservasjon;

/**
 * {@link Stillingsforholdprognosemodus} setter opp batchen til � generere m�nedlige observasjonar
 * p� stillingsforholdniv�.
 * <br>
 * Observasjonane blir seinare (aka utanfor batchen) aggregert til avtaleniv� og mata inn i MySQL for bruk av FAN
 * ved generering av ny eller oppdagerte prognoser.
 * <br>
 * Formatet forventast fasa ut i l�pet av 2015 n�r live-tidsserien blir mata inn i Qlikview / DVH regelmessig.
 *
 * @author Tarjei Skorgenes
 */
public class Stillingsforholdprognosemodus implements Tidsseriemodus {
    private final ThreadLocal<NumberFormat> format = new ThreadLocal<>();

    /**
     * Kolonnenavna for kolonnene som prognoseobservasjonane best�r.
     * <br>
     * Kolonnene er som f�lger:
     * <ol>
     * <li>avtaleId</li>
     * <li>stillingsforholdId</li>
     * <li>observasjonsdato</li>
     * <li>maskinelt_grunnlag</li>
     * <li>premiestatus (ustabil/ust�tta)</li>
     * <li>�rsverk</li>
     * <li>personnummer (uimplementert)</li>
     * </ol>
     *
     * @return ein straum med dei 7 kolonnene som modusen genererer data for p� aggregert niv�
     */
    @Override
    public Stream<String> kolonnenavn() {
        return Stream.of(
                "avtaleId",
                "stillingsforholdId",
                "observasjonsdato",
                "maskinelt_grunnlag",
                "premiestatus",
                "�rsverk",
                "personnummer"
        );
    }

    /**
     * Genererer ein observasjonspublikator som aggregerer alle underlagsperioder pr observasjonsunderlag,
     * til stillingsforhold- og avtaleniv�.
     * <br>
     * Dei aggregerte m�lingane blir serialisert til tekst og lagra via <code>lagring</code>.
     * <br>
     * Merk at sj�lv om personnummer er med som ei kolonne i denne modusen s� blir den ikkje
     * populert med gyldige data sidan informasjon om medlemmet ikkje er inkludert i aggregeringa av observasjonane.
     * <br>
     * Merk og at premiestatusen som blir lista ut ikkje er p�litelig i situasjonar der stillingsforholdet
     * har vore gjennom eit avtalebytte.
     *
     * @param tidsserie tidsserien som publikatoren skal integrere mot
     * @param lagring   backend-systemet som skal ta i mot og lagre unna observasjonane som blir generert
     * @return ein ny observasjonspublikator for prognoseobservasjonar og stillingsforhold- og avtaleniv�
     * @see TidsserieFacade#lagObservasjonsaggregatorPrStillingsforholdOgAvtale(Consumer)
     */
    @Override
    public Observasjonspublikator create(final TidsserieFacade tidsserie, StorageBackend lagring) {
        final Consumer<TidsserieObservasjon> konsument = o -> {
            lagring.lagre(builder -> {
                builder
                        .append(o.avtale().id())
                        .append(';')
                        .append(o.stillingsforhold.id())
                        .append(';')
                        .append(o.observasjonsdato.dato())
                        .append(';')
                        .append(o.maskineltGrunnlag.verdi())
                        .append(';')
                        .append(o.premiestatus().map(Premiestatus::kode).orElse("UKJENT"))
                        .append(';')
                        .append(o.maaling(Aarsverk.class)
                                        .map(Aarsverk::tilProsent)
                                        .map(Prosent::toDouble)
                                        .map(aarsverkformat()::format)
                                        .orElse("0.0")
                        )
                        .append(';')
                        .append("MISSING")
                        .append('\n');
            });
        };
        return tidsserie.lagObservasjonsaggregatorPrStillingsforholdOgAvtale(konsument);
    }

    /**
     * Returnerer regelsettet som skal benyttast ved generering av prognoseobservasjonar.
     *
     * @return eit nytt sett med reglar for prognoseform�l
     * @see PrognoseRegelsett
     */
    @Override
    public Regelsett regelsett() {
        return new PrognoseRegelsett();
    }

    private NumberFormat aarsverkformat() {
        NumberFormat format = this.format.get();
        if (format == null) {
            format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(3);
            format.setMinimumFractionDigits(1);
            this.format.set(format);
        }
        return format;
    }
}