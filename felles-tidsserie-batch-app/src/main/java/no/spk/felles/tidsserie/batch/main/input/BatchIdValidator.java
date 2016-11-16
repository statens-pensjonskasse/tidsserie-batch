package no.spk.felles.tidsserie.batch.main.input;

import static no.spk.felles.tidsserie.batch.core.BatchIdConstants.GRUNNLAGSDATA_PATTERN;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class BatchIdValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value != null && !GRUNNLAGSDATA_PATTERN.matcher(value).matches()) {
            throw new ParameterException("'" + name + "': må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS.");
        }
    }
}
