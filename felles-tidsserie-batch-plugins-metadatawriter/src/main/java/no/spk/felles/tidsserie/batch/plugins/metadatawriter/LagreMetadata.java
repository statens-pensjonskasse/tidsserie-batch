package no.spk.felles.tidsserie.batch.plugins.metadatawriter;

import static java.util.Objects.requireNonNull;
import static no.spk.felles.tidsserie.batch.plugins.metadatawriter.TemplateConfigurationFactory.create;

import java.nio.file.Path;

import no.spk.felles.tidsserie.batch.core.TidsserieGenerertCallback2;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.pensjon.faktura.tjenesteregister.ServiceRegistry;

class LagreMetadata implements TidsserieGenerertCallback2 {
    private final Path logKatalog;
    private final TidsserieBatchArgumenter argumenter;

    LagreMetadata(final Path logKatalog, final TidsserieBatchArgumenter argumenter) {
        this.logKatalog = requireNonNull(logKatalog, "logKatalog er påkrevd, men var null");
        this.argumenter = requireNonNull(argumenter, "argumenter er påkrevd, men var null");
    }

    @Override
    public void tidsserieGenerert(final ServiceRegistry serviceRegistry, final TidsserieGenerertCallback2.Metadata metadata) {
        new MetaDataWriter(create(), logKatalog)
                .createMetadataFile(argumenter, metadata.kjøring, metadata.kjøretid);
    }
}
