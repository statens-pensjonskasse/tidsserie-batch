package no.spk.tidsserie.batch.main.input;

import static no.spk.tidsserie.batch.core.BatchIdConstants.GRUNNLAGSDATA_PATTERN;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class BatchIdValidator {

    public void validate(final String name, final String value, final CommandSpec spec) throws ParameterException {
        if (value != null && !GRUNNLAGSDATA_PATTERN.matcher(value).matches()) {
            throw new ParameterException(
                    new CommandLine(spec),
                    "'" + name + "': må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS."
            );
        }
    }
}
