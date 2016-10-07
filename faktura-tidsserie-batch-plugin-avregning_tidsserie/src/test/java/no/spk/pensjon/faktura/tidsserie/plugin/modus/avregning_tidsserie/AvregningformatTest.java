package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon.avregningsversjon;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsdato.foedselsdato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Personnummer.personnummer;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId.stillingsforhold;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.TreigUUIDRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.UUIDRegel;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.felles.tidsperiode.underlag.PaakrevdAnnotasjonManglarException;
import no.spk.felles.tidsperiode.underlag.Underlag;
import no.spk.felles.tidsperiode.underlag.Underlagsperiode;
import no.spk.felles.tidsperiode.underlag.UnderlagsperiodeBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Enheitstestar for {@link Avregningformat}.
 *
 * @author Tarjei Skorgenes
 */
public class AvregningformatTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    private final Avregningformat format = new Avregningformat();

    @Test
    public void skalHaLikeAntallKolonneISerialiseringaSomIKolonnenavna() {
        final List<String> expected = format.kolonnenavn()
                .collect(toList());
        final List<Object> actual = serialiser(observasjonsdato(), eiPeriodeMedKunObligatoriskeVerdiar())
                .collect(toList());
        assertThat(actual)
                .as("kolonner generert av serialiseringa vs kolonnenavna serialiseringa seier den støttar")
                .hasSameSizeAs(expected);
    }

    @Test
    public void skal_feile_viss_uuid_manglar() {
        forventPaakrevdAnnotasjonFeilForType(UUID.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(UUID.class)
        );
    }

    @Test
    public void skal_feile_viss_observasjonsdato_manglar() {
        forventPaakrevdAnnotasjonFeilForType(Observasjonsdato.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar(),
                (ignorert, p) -> new Underlag(of(p))
        );
    }

    @Test
    public void skal_feile_viss_foedselsnummer_manglar() {
        forventPaakrevdAnnotasjonFeilForType(Foedselsnummer.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(Foedselsnummer.class)
        );
    }

    @Test
    public void skal_feile_viss_stillingsforhold_manglar() {
        forventPaakrevdAnnotasjonFeilForType(StillingsforholdId.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(StillingsforholdId.class)
        );
    }

    @Test
    public void skal_feile_viss_avtale_manglar() {
        forventPaakrevdAnnotasjonFeilForType(AvtaleId.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(AvtaleId.class)
        );
    }

    @Test
    public void skal_feile_viss_avregningsversjon_manglar() {
        forventPaakrevdAnnotasjonFeilForType(Avregningsversjon.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(Avregningsversjon.class)
        );
    }

    private void forventPaakrevdAnnotasjonFeilForType(final Class<?> clazz) {
        e.expect(PaakrevdAnnotasjonManglarException.class);
        e.expectMessage(clazz.getSimpleName());
    }

    private Observasjonsdato observasjonsdato() {
        return new Observasjonsdato(dato("2000.01.31"));
    }

    private Stream<Object> serialiser(final Observasjonsdato dato, final UnderlagsperiodeBuilder builder) {
        return serialiser(dato, builder, this::underlag);
    }

    private Stream<Object> serialiser(final Observasjonsdato dato, final UnderlagsperiodeBuilder builder,
                                      final BiFunction<Observasjonsdato, Underlagsperiode, Underlag> underlag) {
        final Underlagsperiode periode = builder.bygg();
        return format.serialiser(
                underlag.apply(dato, periode),
                periode
        )
                .collect(toList())
                .stream();
    }

    private Underlag underlag(final Observasjonsdato dato, final Underlagsperiode periode) {
        return new Underlag(of(periode))
                .annoter(Observasjonsdato.class, dato);
    }

    private UnderlagsperiodeBuilder eiPeriodeMedKunObligatoriskeVerdiar() {
        return new UnderlagsperiodeBuilder()
                .fraOgMed(dato("2000.01.01"))
                .tilOgMed(dato("2000.01.31"))
                .med(new Foedselsnummer(
                        foedselsdato(19800101),
                        personnummer(1)
                ))
                .med(stillingsforhold(1L))
                .med(avtaleId(223344L))
                .med(avregningsversjon(29))
                .med(UUID.randomUUID())
                ;
    }
}