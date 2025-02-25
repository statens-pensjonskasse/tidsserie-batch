package no.spk.tidsserie.batch.core.kommandolinje;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class AvailableProcessors implements AfterEachCallback {

    void overstyr(final int antall) {
        AntallProsessorar.overstyr(() -> antall);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        AntallProsessorar.reset();
    }
}
