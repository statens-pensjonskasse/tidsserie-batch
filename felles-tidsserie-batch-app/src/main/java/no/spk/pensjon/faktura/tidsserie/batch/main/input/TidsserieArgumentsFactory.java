package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import no.spk.faktura.input.ProgramArgumentsFactory;

/**
 * Util-klasse med metoden {@link #create} som produserer @{link ProgramArguments} fra en array med {@code String[]}
 *
 * @author Snorre E. Brekke - Computas
 * @see ProgramArguments
 */
public final class TidsserieArgumentsFactory extends ProgramArgumentsFactory<ProgramArguments> {
    public TidsserieArgumentsFactory() {
        super(ProgramArguments.class, new PostParseValidation());
    }
}
