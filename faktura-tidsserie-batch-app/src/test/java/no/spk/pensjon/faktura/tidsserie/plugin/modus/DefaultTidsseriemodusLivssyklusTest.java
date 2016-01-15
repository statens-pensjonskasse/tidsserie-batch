package no.spk.pensjon.faktura.tidsserie.plugin.modus;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import no.spk.pensjon.faktura.tidsserie.batch.ServiceRegistryRule;
import no.spk.pensjon.faktura.tidsserie.core.AgentInitializer;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.util.Services;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author Snorre E. Brekke - Computas
 */
public class DefaultTidsseriemodusLivssyklusTest {
    @Rule
    public ServiceRegistryRule services = new ServiceRegistryRule();

    @Test
    public void skal_registrere_agent_initializer() throws Exception {
        services.registrer(StorageBackend.class, mock(StorageBackend.class));

        new DefaultTidsseriemodusLivssyklus(emptyList()).start(services.registry());

        assertThat(Services.lookupAll(services.registry(), AgentInitializer.class)
                .filter(i -> i instanceof KolonnenavnPerPartisjon)
                .findAny()
        ).isPresent();
    }
}