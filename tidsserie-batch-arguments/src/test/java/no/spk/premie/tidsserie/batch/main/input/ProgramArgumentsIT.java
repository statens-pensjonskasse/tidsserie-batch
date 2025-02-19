package no.spk.premie.tidsserie.batch.main.input;

import static no.spk.premie.tidsserie.batch.core.UttrekksId.uttrekksId;
import static no.spk.premie.tidsserie.batch.core.kommandolinje.AntallProsessorar.antallProsessorar;
import static no.spk.premie.tidsserie.batch.core.kommandolinje.AntallProsessorar.standardAntallProsessorar;
import static no.spk.premie.tidsserie.batch.main.input.Datoar.dato;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

import no.spk.felles.tidsperiode.Aarstall;
import no.spk.felles.tidsperiode.underlag.Observasjonsperiode;
import no.spk.premie.tidsserie.batch.core.UttrekksId;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.StringAssert;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SoftAssertionsExtension.class)
public class ProgramArgumentsIT {

    @RegisterExtension
    public final ServiceRegistryExtension registry = new ServiceRegistryExtension();

    @RegisterExtension
    public final ModusExtension modus = new ModusExtension();

    @InjectSoftAssertions
    private SoftAssertions softly;

    private final ProgramArguments args = new ProgramArguments();

    @BeforeEach
    void _before(@TempDir File temp) throws IOException {
        args.innkatalog = newFolder(temp, "inn").toPath();
        args.utkatalog = newFolder(temp, "ut").toPath();
        args.logkatalog = newFolder(temp, "log").toPath();
        args.beskrivelse = "Min beskrivelse";
        args.fraAar = 2009;
        args.tilAar = 2018;
        args.nodes = antallProsessorar(1);
        args.antallNoderForPrinting = 1;
        args.slettLogEldreEnn = 14;
        args.kjoeretid = "0401";
        args.sluttidspunkt = LocalTime.MAX;
        args.uttrekk = uttrekksId("grunnlagsdata_2017-01-01_00-00-00-00");
    }

    @Test
    void skal_oppsummere_alle_argument_med_verdiar_i_toString() {
        modus.support("min_modus");
        Modus.parse("min_modus").ifPresent(m -> args.modus = m);

        assertToString()
                .contains("-i: " + args.innkatalog)
                .contains("-o: " + args.utkatalog)
                .contains("-log: " + args.logkatalog)
                .contains("-b: Min beskrivelse")
                .contains("-id: grunnlagsdata_2017-01-01_00-00-00-00")
                .contains("-fraAar: 2009")
                .contains("-tilAar: 2018")
                .contains("-n: 1")
                .contains("-m: min_modus")
                .contains("-kjoeretid: 0401")
                .contains("-sluttid: 23:59")
                .contains("-slettLog: 14")
                .doesNotContain("-help")
                .doesNotContain("-hjelp")
        ;
    }

    @Test
    void skal_generere_observasjonsperiode_fra_1_januar_til_31_desember_i_fra_og_til_aarstalla() {
        args.fraAar = 2010;
        args.tilAar = 2015;
        assertThat(args.observasjonsperiode())
                .isEqualTo(new Observasjonsperiode(dato("2010.01.01"), dato("2015.12.31")));
    }

    @Test
    void skal_bruke_1_mindre_enn_antall_tilgjengelide_prosessorar_som_standardverdi() {
        assertThat(new ProgramArguments().nodes)
                .as("standardverdi når antall prosessorar ikkje blir overstyrt på kommandolinja")
                .isEqualTo(standardAntallProsessorar());
    }

    @Test
    void skal_registrere_observasjonsperiode_i_tenesteregisteret() {
        args.fraAar = 1917;
        args.tilAar = 2037;

        args.registrer(registry.registry());

        registry
                .assertTenesterAvType(Observasjonsperiode.class)
                .hasSize(1)
                .containsOnly(
                        new Observasjonsperiode(
                                new Aarstall(1917).atStartOfYear(),
                                new Aarstall(2037).atEndOfYear()
                        )
                );
    }

    @Test
    void skal_ikkje_velge_eit_uttrekk_automatisk_viss_brukaren_allereie_har_angitt_kva_uttrekk_som_skal_brukast() {
        final UttrekksId expected = uttrekksId("grunnlagsdata_1970-01-01_00-00-00-00");
        brukUttrekk(expected);

        args.velgUttrekkVissIkkeAngitt(inn -> uttrekksId("grunnlagsdata_1980-01-01_00-00-00-00"));

        assertThat(args.uttrekk).isSameAs(expected);
    }

    @Test
    void skal_velge_eit_uttrekk_automatisk_dersom_brukaren_ikkje_angir_kva_uttrekk_som_skal_brukast() {
        final UttrekksId expected = uttrekksId("grunnlagsdata_1980-01-01_00-00-00-00");
        brukUttrekk(null);

        args.velgUttrekkVissIkkeAngitt(inn -> expected);

        assertThat(args.uttrekk).isSameAs(expected);
    }

    private void brukUttrekk(final UttrekksId uttrekk) {
        args.uttrekk = uttrekk;
    }

    private StringAssert assertToString() {
        return softly.assertThat(args.toString());
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}