package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import static java.util.stream.Stream.of;
import static no.spk.pensjon.faktura.tidsserie.Datoar.dato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId.avtaleId;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsdato.foedselsdato;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Personnummer.personnummer;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId.stillingsforhold;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Foedselsnummer;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.StillingsforholdId;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.Observasjonsdato;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.PaakrevdAnnotasjonManglarException;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagsperiodeBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Enheitstestar for {@link Datavarehusformat}.
 *
 * @author Tarjei Skorgenes
 */
public class DatavarehusformatTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    private final Datavarehusformat format = new Datavarehusformat();

    @Test
    public void skalFeileVissObservasjonsdatoManglar() {
        forventPaakrevdAnnotasjonFeilForType(Observasjonsdato.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar(),
                (ignorert, p) -> new Underlag(of(p))
        );
    }

    @Test
    public void skalFeileVissFoedselsnummerManglar() {
        forventPaakrevdAnnotasjonFeilForType(Foedselsnummer.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(Foedselsnummer.class)
        );
    }

    @Test
    public void skalFeileVissStillingsforholdManglar() {
        forventPaakrevdAnnotasjonFeilForType(StillingsforholdId.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(StillingsforholdId.class)
        );
    }

    @Test
    public void skalFeileVissAvtaleManglar() {
        forventPaakrevdAnnotasjonFeilForType(AvtaleId.class);

        serialiser(
                observasjonsdato(),
                eiPeriodeMedKunObligatoriskeVerdiar()
                        .uten(AvtaleId.class)
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
        );
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
                        foedselsdato(dato("1980.01.01")),
                        personnummer(1)
                ))
                .med(stillingsforhold(1L))
                .med(avtaleId(223344L));
    }
}