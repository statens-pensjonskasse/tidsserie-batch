package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;


import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.util.TemporaryFolderWithDeleteVerification;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagskriverIT {

    @Rule
    public TemporaryFolder temp = new TemporaryFolderWithDeleteVerification();

    @Test
    public void skal_skrive_underlag_til_fil() throws Exception {
        Path outdir = temp.newFolder().toPath();
        FileTemplate fileTemplate = new FileTemplate(outdir, "avtaler-", ".csv");

        PeriodeTypeTestFactory tidsperiodeFactory = new PeriodeTypeTestFactory();
        AvtaleunderlagFactory factory = new AvtaleunderlagFactory(tidsperiodeFactory, new AvtaleunderlagRegelsett());

        final AvtaleId avtaleId = avtaleId(1L);
        tidsperiodeFactory.addPerioder(
                new Avtaleperiode(dato("2015.01.01"), empty(), avtaleId, ArbeidsgiverId.valueOf(2))
        );

        final Stream<Underlag> underlag = factory.lagAvtaleunderlag(
                new Observasjonsperiode(dato("2015.01.01"), dato("2015.12.31")),
                new Uttrekksdato(dato("2016.01.01"))
        );

        final Avtaleunderlagformat avtaleformat = new Avtaleunderlagformat();
        Avtaleunderlagskriver skriver = new Avtaleunderlagskriver(fileTemplate, avtaleformat);
        skriver.skrivAvtaleunderlag(underlag);

        try (Stream<Path> filestream = Files.list(outdir)){
            final List<Path> result = filestream.collect(toList());
            assertThat(result).hasSize(1);
            Path output = result.get(0);

            assertThat(output.getFileName().toString()).matches("avtaler-1-.+-.+-.+-.+-.+\\.csv");

            final List<String> outputlines = Files.readAllLines(output);
            assertThat(outputlines).hasSize(2);
            assertThat(outputlines.get(0)).isEqualTo(avtaleformat.kolonnenavn().collect(joining(";")));
            assertThat(outputlines.get(1)).startsWith("2015;2015-01-01;2015-12-31;1");
        }

    }
}