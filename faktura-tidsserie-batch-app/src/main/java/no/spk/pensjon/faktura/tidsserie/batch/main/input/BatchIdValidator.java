package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import static no.spk.pensjon.faktura.tidsserie.batch.main.input.BatchIdMatcher.GRUNNLAGSDATA_PATTERN;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class BatchIdValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value != null && !GRUNNLAGSDATA_PATTERN.matcher(value).matches()) {
            throw new ParameterException("'" + name + "': m� oppgis p� formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS.");
        }
    }
}
