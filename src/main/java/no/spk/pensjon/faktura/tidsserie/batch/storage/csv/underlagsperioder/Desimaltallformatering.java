package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.underlagsperioder;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@link Desimaltallformatering} representerer ei trådsikker, låsfri teneste for formatering av
 * desimaltall.
 * <br>
 * Tenesta benyttar seg av {@link NumberFormat} som er ikkje er trådsikker og dermed ikkje kan
 * delast på tvers av trådar. Men ein ønskjer likevel å cache formata som blir oppretta, med eit format
 * pr type formatering basert på antall desimalar som krevest.
 * <br>
 * For å unngå deling/låsing av NumberFormat genererer derfor tenesta ein cache med eit NumberFormat pr antall desimalar
 * pr tråd.
 *
 * @author Tarjei Skorgenes
 */
class Desimaltallformatering {
    private final ThreadLocal<Map<Integer, NumberFormat>> desimalformat = ThreadLocal.withInitial(HashMap::new);

    String formater(final double value, final int antallDesimaler) {
        return desimalFormat(antallDesimaler).format(value);
    }

    NumberFormat desimalFormat(final int antallDesimaler) {
        return desimalformat.get().computeIfAbsent(antallDesimaler, antall -> {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
            format.setRoundingMode(RoundingMode.HALF_UP);
            format.setMaximumFractionDigits(antall);
            format.setMinimumFractionDigits(antall);
            return format;
        });
    }
}
