package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale.avtale;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiekategori;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Premiestatus;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Enheitstestar for {@link no.spk.pensjon.faktura.tidsserie.storage.csv.AvtaleversjonOversetter}.
 *
 * @author Tarjei Skorgenes
 */
public class AvtaleversjonOversetterTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    private final AvtaleversjonOversetter oversetter = new AvtaleversjonOversetter();

    @Test
    public void skalOversetteRadTilAvtaleversjon() {
        final Avtaleversjon versjon = oversett("AVTALEVERSJON;123456;1917.01.01;;2007.01.01;IPB;FSA;123456789");
        assertThat(versjon.avtale()).isEqualTo(avtaleId(123456));
        assertThat(versjon.fraOgMed()).as("fra og med-dato").isEqualTo(dato("1917.01.01"));
        assertThat(versjon.tilOgMed()).as("til og med-dato").isEqualTo(empty());

        final Avtale avtale = konverter(versjon);
        assertThat(avtale.premiestatus()).isEqualTo(Premiestatus.valueOf("IPB"));
        assertThat(avtale.premiekategori()).isEqualTo(Premiekategori.parse("FSA"));
    }

    @Test
    public void skalFeileVissPremiekategoriErUkjent() {
        e.expect(IllegalStateException.class);
        e.expectMessage("LOL");
        e.expectMessage("er ikkje ein gyldig premiekategori");

        oversett("AVTALEVERSJON;223344;1917.01.01;;2007.01.01;FIK;LOL;987654321");
    }

    private Avtaleversjon oversett(final String linje) {
        final String[] rad = linje.split(";");
        final List<String> verdier = asList(rad);
        return oversetter.oversett(verdier);
    }

    private Avtale konverter(final Avtaleversjon versjon) {
        final Avtale.AvtaleBuilder builder = avtale(versjon.avtale());
        versjon.populer(builder);
        return builder.bygg();
    }
}