package no.spk.premie.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar;

import no.spk.premie.tidsserie.batch.core.grunnlagsdata.Partisjonsnummer;
import no.spk.premie.tidsserie.batch.core.medlem.TidsserieContext;

class Context implements TidsserieContext {
    private final Meldingar meldingar = new Meldingar();

    private final int serienummer;

    Context(final Partisjonsnummer nummer) {
        this.serienummer = Math.toIntExact(nummer.index() + 1);
    }

    @Override
    public long getSerienummer() {
        return serienummer;
    }

    @Override
    public void emitError(final Throwable t) {
        meldingar.emitError(t);
    }

    void emit(final String key) {
        meldingar.emit(key);
    }

    void inkluderFeilmeldingarFrå(final Runnable handling) {
        try {
            handling.run();
        } catch (final RuntimeException e) {
            this.emitError(e);
        }
    }

    Meldingar meldingar() {
        return meldingar;
    }
}
