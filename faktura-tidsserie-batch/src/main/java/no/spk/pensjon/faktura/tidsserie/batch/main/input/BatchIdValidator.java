package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.util.regex.Pattern;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class BatchIdValidator implements IParameterValidator {

    private static final Pattern BATCH_ID_PATTERN = BatchIdMatcher.createBatchIdPattern("grunnlagsdata_");

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value != null && !BATCH_ID_PATTERN.matcher(value).matches()) {
            throw new ParameterException("'" + name + "': må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS.");
        }
    }
}
