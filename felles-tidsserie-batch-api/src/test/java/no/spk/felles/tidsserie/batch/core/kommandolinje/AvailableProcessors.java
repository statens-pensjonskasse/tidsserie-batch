package no.spk.felles.tidsserie.batch.core.kommandolinje;

import java.util.function.Supplier;

import org.junit.rules.ExternalResource;

class AvailableProcessors extends ExternalResource {
    @Override
    protected void after() {
        AntallProsessorar.reset();
    }

    void overstyr(final int antall) {
        AntallProsessorar.overstyr(() -> antall);
    }
}
