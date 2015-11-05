package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.avregning;

import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsperiode.avregningsperiode;
import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon.avregningsversjon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import no.spk.pensjon.faktura.tidsserie.batch.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.batch.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AvregningsRegelsett;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.GenerellTidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonspublikator;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Annoterbar;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.storage.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.storage.disruptor.ObservasjonsEvent;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.assertj.core.api.AbstractObjectArrayAssert;
import org.junit.Rule;
import org.junit.Test;

public class AvregningTidsseriemodusTest {
    @Rule
    public final TemporaryFolderWithDeleteVerification temp = new TemporaryFolderWithDeleteVerification();

    private final TidsperiodeFactory factory = mock(TidsperiodeFactory.class);

    private final AvregningTidsseriemodus modus = new AvregningTidsseriemodus();

    @Test
    public void skal_inkludere_avregningsperiode_i_referansedata() {
        final Avregningsperiode expected = avregningsperiode()
                .fraOgMed(new Aarstall(2015))
                .tilOgMed(new Aarstall(2015))
                .versjonsnummer(avregningsversjon(827))
                .bygg();
        when(factory.perioderAvType(Avregningsperiode.class)).thenReturn(Stream.of(expected));
        assertReferansedata().contains(expected);
    }

    @Test
    public void skal_inkludere_reglar_i_referansedata() {
        assertThat(
                reglarFra(
                        referansedata()
                                .filter(Regelperiode.class::isInstance)
                                .map(p -> (Regelperiode<?>) p)
                )
        )
                .as("beregningsreglar fra referansedata-periodene")
                .containsAll(
                        reglarFra(modus.regelsett().reglar())
                );

    }

    @Test
    public void skal_inkludere_loennsdata_i_referansedata() {
        final Tidsperiode<?> expected = new GenerellTidsperiode(dato("2015.05.01"), empty());
        when(factory.loennsdata()).thenReturn(Stream.of(expected));
        assertReferansedata().contains(expected);
    }

    @Test
    public void skal_annotere_observasjonsunderlaga_med_tidsserienummer_basert_paa_dagens_dato() {
        final LocalDate dato = now();
        final Tidsserienummer expected = Tidsserienummer.genererForDato(dato);
        final List<Underlag> resultat = new ArrayList<>();

        final Observasjonspublikator publikator = modus.nyPublikator(Stream::of, resultat::add);
        publikator.publiser(Stream.of(new Underlag(Stream.empty()).annoter(Observasjonsdato.class, new Observasjonsdato(dato("2015.12.31")))));
        assertThat(resultat).as("underlagene mottatt av publikatoren").hasSize(1);

        resultat.forEach(u -> {
            assertThat(u.valgfriAnnotasjonFor(Tidsserienummer.class)).isEqualTo(of(expected));
        });
    }

    @Test
    public void skal_inkludere_oversetter_for_avregningsperioder() throws IOException {
        final File file = temp.newFile("avregningsperioder.csv.gz");
        try (final OutputStream output = new GZIPOutputStream(new FileOutputStream(file))) {
            output.write("AVREGNINGSPERIODE;2015;2015;98".getBytes("ASCII"));
        }

        final GrunnlagsdataRepository repository = modus.repository(temp.getRoot().toPath());
        assertThat(
                repository
                        .referansedata()
                        .filter(Avregningsperiode.class::isInstance)
                        .collect(toList())
        )
                .hasSize(1);

    }

    @Test
    public void skal_filtrere_bort_observasjonsunderlag_som_ikkje_er_observert_siste_dag_i_aaret() {
        final List<Underlag> resultat = new ArrayList<>();

        final Observasjonspublikator publikator = modus.nyPublikator(Stream::of, resultat::add);
        publikator.publiser(
                Stream.of(
                        new Underlag(Stream.empty()).annoter(Observasjonsdato.class, new Observasjonsdato(dato("2015.11.30"))),
                        new Underlag(Stream.empty()).annoter(Observasjonsdato.class, new Observasjonsdato(dato("2015.12.31"))),
                        new Underlag(Stream.empty()).annoter(Observasjonsdato.class, new Observasjonsdato(dato("2016.01.31"))),
                        new Underlag(Stream.empty()).annoter(Observasjonsdato.class, new Observasjonsdato(dato("2016.12.31")))
                )
        );
        assertThat(resultat).as("underlagene mottatt av publikatoren").hasSize(2);
        assertThat(resultat.stream().map(u -> u.annotasjonFor(Observasjonsdato.class)).toArray())
                .containsOnly(new Observasjonsdato(dato("2015.12.31")), new Observasjonsdato(dato("2016.12.31")));
    }

    @Test
    public void skal_ikke_benytte_serienummer() {
        final ObservasjonsEvent event = new ObservasjonsEvent();
        modus.lagre(consumer -> consumer.accept(event), "...");

        assertThat(event.serienummer()).isEmpty();
    }

    @Test
    public void skal_sende_serialisert_linje_vidare_for_lagring() {
        final String expected = "X;Y;Z";
        final ObservasjonsEvent event = new ObservasjonsEvent();
        modus.lagre(consumer -> consumer.accept(event), expected);

        assertThat(event.buffer.toString()).as("linje").isEqualTo(expected + "\n");
    }

    @Test
    public void skal_bruke_avregnings_regelsettet() {
        assertThat(modus.regelsett()).isInstanceOf(AvregningsRegelsett.class);
    }

    @Test
    public void skal_ikke_skrive_noe_naar_partisjon_initisialiseres() {
        final ObservasjonsEvent event = new ObservasjonsEvent();
        modus.partitionInitialized(1, c -> c.accept(event));
        assertThat(event.buffer.toString()).isEmpty();
    }

    private AbstractObjectArrayAssert<?, Object> assertReferansedata() {
        return assertThat(referansedata().toArray()).as("referansedata-perioder");
    }

    private Stream<Tidsperiode<?>> referansedata() {
        return modus.referansedata(factory);
    }

    private List<Class<?>> reglarFra(final Stream<? extends Regelperiode<?>> perioder) {
        return perioder.map(this::capture)
                .map(Object::getClass)
                .collect(toList());
    }

    private Object capture(Regelperiode<?> periode) {
        class CaptureAnnotasjon implements Annoterbar<CaptureAnnotasjon> {
            Object verdi;

            @Override
            public <T> CaptureAnnotasjon annoter(final Class<? extends T> type, final T verdi) {
                this.verdi = verdi;
                return this;
            }

            @Override
            public CaptureAnnotasjon annoterFra(final CaptureAnnotasjon kilde) {
                return this;
            }
        }
        final CaptureAnnotasjon capture = new CaptureAnnotasjon();
        periode.annoter(capture);
        return capture.verdi;
    }
}