package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import static java.lang.String.format;

import no.spk.tidsserie.batch.core.medlem.GenererTidsserieCommand;

class IngenGenererTidsserieKommandoRegistrertException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    @Override
    public String getMessage() {
        return format(
                "Det eksisterer ikkje noko teneste av type %s i tenesteregisteret",
                GenererTidsserieCommand.class.getSimpleName()
        );
    }
}
