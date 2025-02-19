package no.spk.premie.tidsserie.batch.core;

import java.util.regex.Pattern;

import no.spk.faktura.input.BatchId;

/**
 * Ofte brukte konstantverdiar brukt i forbindelse med lokalisering av inn-katalog og log-katalog
 * for batchen.
 *
 * @author Snorre E. Brekke - Computas
 */
public class BatchIdConstants {
    public static final String TIDSSERIE_PREFIX = "tidsserie_";
    public static final String GRUNNLAGSDATA_PREFIX = "grunnlagsdata_";

    public static final Pattern TIDSSERIE_PATTERN = BatchId.createBatchIdPattern(TIDSSERIE_PREFIX);
    public static final Pattern GRUNNLAGSDATA_PATTERN = BatchId.createBatchIdPattern(GRUNNLAGSDATA_PREFIX);
}
