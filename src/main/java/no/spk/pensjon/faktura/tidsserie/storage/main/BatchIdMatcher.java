package no.spk.pensjon.faktura.tidsserie.storage.main;

import java.util.regex.Pattern;

/**
 * @author Snorre E. Brekke - Computas
 */
public class BatchIdMatcher {
    public static Pattern createBatchIdPattern(String batchIdPrefix) {
        return Pattern.compile("^(" + batchIdPrefix + "\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{2})$");
    }
}
