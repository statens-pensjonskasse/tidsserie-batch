package no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag;

import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode.avtaleperiode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import no.spk.pensjon.faktura.tidsserie.batch.core.GrunnlagsdataRepository;
import no.spk.pensjon.faktura.tidsserie.batch.core.Katalog;
import no.spk.pensjon.faktura.tidsserie.batch.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.batch.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.batch.core.TidsserieGenerertCallback;
import no.spk.pensjon.faktura.tidsserie.batch.modus.avtaleunderlag.Avtaleunderlagmodus.ReferansedataCSVInput;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tjenesteregister.Constants;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagmodusTest {
    @Rule
    public ServiceRegistryRule services = new ServiceRegistryRule();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private Avtaleunderlagmodus modus;

    private Path innKatalog;

    @Before
    public void setUp() throws Exception {
        modus = new Avtaleunderlagmodus();

        services.registrer(Observasjonsperiode.class, new Observasjonsperiode(dato("2015.01.01"), dato("2015.01.01")));
        services.registrer(StorageBackend.class, mock(StorageBackend.class));
        innKatalog = temp.newFolder("grunnlagsdata_2016-01-01_01-01-01-01").toPath();
        services.registry().registerService(
                Path.class,
                innKatalog,
                Katalog.GRUNNLAGSDATA.egenskap()
        );
        services.registry().registerService(
                Path.class,
                temp.newFolder("ut").toPath(),
                Katalog.UT.egenskap()
        );
    }

    @Test
    public void skal_overstyre_medlemsdata_lesing_av_ytelseshensyn() throws IOException {
        writeAscii("medlemsdata.csv.gz", "YADA;YADA;YADA");

        GrunnlagsdataRepository repository = modus.repository(innKatalog);
        assertThat(repository).isInstanceOf(ReferansedataCSVInput.class);

        assertThat(
                repository
                        .medlemsdata()
                        .collect(Collectors.toList())
        )
                .isEmpty();
    }

    @Test
    public void skal_registrere_repository_som_overstyrer_standardtenesta() throws IOException {
        services.registrer(Path.class, temp.getRoot().toPath(), Katalog.GRUNNLAGSDATA.egenskap());

        modus.registerServices(services.registry());

        services.assertFirstService(GrunnlagsdataRepository.class).isPresent();

        assertThat(
                services
                        .registry()
                        .getServiceReference(GrunnlagsdataRepository.class)
                        .flatMap(r -> r.getProperty(Constants.SERVICE_RANKING))
        )
                .isEqualTo(of("1000"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void skal_lagre_avtaleunderlag() throws Exception {
        services.registrer(TidsperiodeFactory.class, mock(TidsperiodeFactory.class));
        Underlagskriver skriver = mock(Underlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        modus.lagTidsserie(services.registry());

        verify(skriver).lagreUnderlag(any(Stream.class));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void skal_returnere_antall_avtaler_prosessert() throws Exception {
        PeriodeTypeTestFactory tidsperiodeFactory = new PeriodeTypeTestFactory();
        services.registrer(TidsperiodeFactory.class, tidsperiodeFactory);

        Underlagskriver skriver = mock(Underlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        tidsperiodeFactory.addPerioder(
                enAvtalepriode(AvtaleId.avtaleId(1L)),
                enAvtalepriode(AvtaleId.avtaleId(2L))
        );
        final Map<String, Integer> resultat = modus.lagTidsserie(services.registry());

        assertThat(resultat.get("avtaler")).isEqualTo(2);
    }

    @Test
    public void skal_registrere_liveTidsserieAvslutter() throws IOException {
        services.registrer(StorageBackend.class, mock(StorageBackend.class));

        modus.registerServices(services.registry());

        services.assertFirstService(TidsserieGenerertCallback.class).isPresent();

        assertThat(
                services.firstService(TidsserieGenerertCallback.class).get()
        )
                .isInstanceOf(AvtaleunderlagAvslutter.class);
    }

    @Test
    public void skal_lage_error_meldinger() {
        PeriodeTypeTestFactory tidsperiodeFactory = new PeriodeTypeTestFactory();
        services.registrer(TidsperiodeFactory.class, tidsperiodeFactory);

        Underlagskriver skriver = mock(Underlagskriver.class);
        modus.avtaleunderlagsskriver(skriver);

        tidsperiodeFactory.addPerioder(
                enAvtalepriode(AvtaleId.avtaleId(1L)),
                enAvtalepriode(AvtaleId.avtaleId(1L))
        );
        final Map<String, Integer> resultat = modus.lagTidsserie(services.registry());

        assertThat(resultat).containsKey("errors_message_Underlagsperioda er kobla til meir enn ei tidsperiode av type Avtaleperiode, vi forventa berre 1 kobling av denne typen.\n" +
                "Koblingar:\n" +
                "- Avtale[2015-01-01->,avtale 1,arbeidsgiver 2]\n" +
                "- Avtale[2015-01-01->,avtale 1,arbeidsgiver 2]\n");
    }

    private Avtaleperiode enAvtalepriode(AvtaleId avtaleId) {
        return avtaleperiode(avtaleId)
                .fraOgMed(dato("2015.01.01"))
                .arbeidsgiverId(ArbeidsgiverId.valueOf(2))
                .bygg();
    }


    private File writeAscii(String fileName, String innhold) throws IOException {
        final File file = innKatalog.resolve(fileName).toFile();
        try (final OutputStream output = new GZIPOutputStream(new FileOutputStream(file))) {
            output.write(innhold.getBytes("ASCII"));
        }
        return file;
    }
}