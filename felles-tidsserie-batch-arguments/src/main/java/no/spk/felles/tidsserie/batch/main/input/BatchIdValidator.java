package no.spk.felles.tidsserie.batch.main.input;

import static no.spk.felles.tidsserie.batch.core.BatchIdConstants.GRUNNLAGSDATA_PATTERN;

import no.spk.faktura.input.DummyCommand;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public class BatchIdValidator {

    public void validate(final String name, final String value) throws ParameterException {
        if (value != null && !GRUNNLAGSDATA_PATTERN.matcher(value).matches()) {
            throw new ParameterException(
                    new CommandLine(new DummyCommand()),
                    "'" + name + "': må oppgis på formatet grunnlagsdata_yyyy-MM-dd_HH-mm-ss-SS."
            );
        }
    }
}
