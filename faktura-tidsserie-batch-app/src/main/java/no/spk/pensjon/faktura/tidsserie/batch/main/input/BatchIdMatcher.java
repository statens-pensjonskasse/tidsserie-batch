package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.util.regex.Pattern;

/**
 * @author Snorre E. Brekke - Computas
 */
public class BatchIdMatcher {
    public static Pattern TIDSSERIE_PATTERN = createBatchIdPattern(BatchId.ID_PREFIX);
    public static Pattern GRUNNLAGSDATA_PATTERN = createBatchIdPattern("grunnlagsdata_");

    private static Pattern createBatchIdPattern(String batchIdPrefix) {
        return Pattern.compile("^(" + batchIdPrefix + "\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{2})$");
    }
}
