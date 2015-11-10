package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsavtaleperiode.avregningsavtaleperiode;

import java.util.function.Consumer;

import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsavtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

/**
 * Mapper for konvertering av rader fra CSV-filer for avregningsavtaleperiode til instanser av
 * {@link Avregningsavtaleperiode}.
 * <br>
 * CSV-radene må benytte typeindikatoren {@value #TYPEINDIKATOR} for av oversetteren skal kunne identifisere rada som
 * ei avregningsperiode, og dermed kunne oversette den.
 *
 * @author Snorre E. Brekke
 * @see Avregningsavtaleperiode
 * @since 1.2.0
 */
public class AvregningsavtaleperiodeOversetter extends ReflectiveCsvOversetter<AvregningsavtaleperiodeCsv, Avregningsavtaleperiode> implements CsvOversetter<Avregningsavtaleperiode> {
    /**
     * Typeindikatoren som CSV-radene må benytte for at oversetteren skal kunne oversette de.
     */
    public static final String TYPEINDIKATOR = "AVREGNINGSAVTALE";

    private Consumer<AvtaleId> avtaleIdConsumer = a -> {};

    public AvregningsavtaleperiodeOversetter() {
        super(TYPEINDIKATOR, AvregningsavtaleperiodeCsv.class);
    }

    public AvregningsavtaleperiodeOversetter(Consumer<AvtaleId> avtaleIdConsumer) {
        super(TYPEINDIKATOR, AvregningsavtaleperiodeCsv.class);
        this.avtaleIdConsumer = avtaleIdConsumer;
    }

    /**
     * Konverterer innholdet fra {@code rad} til ei ny {@link Avregningsavtaleperiode}.
     *
     * @param rad CSV-rada som verdiane til avregningsavtaleperioda skal hentast frå
     * @return ei ny avregningsavtaleperiode, populert med verdiar frå {@code rad}
     */
    @Override
    protected Avregningsavtaleperiode transformer(final AvregningsavtaleperiodeCsv rad) {
        final Avregningsavtaleperiode avregningsavtaleperiode = avregningsavtaleperiode()
                .fraOgMed(
                        rad.premieAarFra.map(Integer::valueOf).map(Aarstall::new).get()
                )
                .tilOgMed(
                        rad.premieAarTil.map(Integer::valueOf).map(Aarstall::new).get()
                )
                .versjonsnummer(
                        rad.avregningsversjon.map(Integer::valueOf).map(Avregningsversjon::avregningsversjon).get()
                )
                .avtale(
                        rad.avtale.map(Integer::valueOf).map(AvtaleId::valueOf).get()
                )
                .bygg();
        avtaleIdConsumer.accept(avregningsavtaleperiode.avtale());
        return avregningsavtaleperiode;
    }
}
