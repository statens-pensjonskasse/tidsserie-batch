package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsperiode.avregningsperiode;

import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avregning.Avregningsversjon;
import no.spk.felles.tidsperiode.Aarstall;

/**
 * Mapper for konvertering av rader fra CSV-filer for avregningsperioder til instanser av
 * {@link Avregningsperiode}.
 * <br>
 * CSV-radene må benytte typeindikatoren {@value #TYPEINDIKATOR} for av oversetteren skal kunne identifisere rada som
 * ei avregningsperiode, og dermed kunne oversette den.
 *
 * @author Tarjei Skorgenes
 * @see AvregningsperiodeCsv
 * @since 1.2.0
 */
public class AvregningsperiodeOversetter extends ReflectiveCsvOversetter<AvregningsperiodeCsv, Avregningsperiode> implements CsvOversetter<Avregningsperiode> {
    /**
     * Typeindikatoren som CSV-radene må benytte for at oversetteren skal kunne oversette de.
     */
    public static final String TYPEINDIKATOR = "AVREGNINGSPERIODE";

    public AvregningsperiodeOversetter() {
        super(TYPEINDIKATOR, AvregningsperiodeCsv.class);
    }

    /**
     * Konverterer innholdet fra {@code rad} til ei ny {@link Avregningsperiode}.
     *
     * @param rad CSV-rada som verdiane til avregningsperioda skal hentast frå
     * @return ei ny avregningsperiode, populert med verdiar frå {@code rad}
     */
    @Override
    protected Avregningsperiode transformer(final AvregningsperiodeCsv rad) {
        return avregningsperiode()
                .fraOgMed(
                        rad.premieAarFra.map(Integer::valueOf).map(Aarstall::new).get()
                )
                .tilOgMed(
                        rad.premieAarTil.map(Integer::valueOf).map(Aarstall::new).get()
                )
                .versjonsnummer(
                        rad.avregningsversjon.map(Integer::valueOf).map(Avregningsversjon::avregningsversjon).get()
                )
                .bygg();
    }
}
