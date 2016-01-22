package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static java.util.stream.Collectors.joining;

import java.util.function.Consumer;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.core.StorageBackend;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;

/**
 * Har i oppgave å skrive resultatet av {@link AvtaleunderlagFactory} matet inn til
 * {@link Avtaleunderlagformat} som csv-rader til {@link StorageBackend}.
 *
 * @author Snorre E. Brekke - Computas
 */
class Underlagskriver {
    private final CSVFormat outputFormat;
    private final StorageBackend storage;

    public Underlagskriver(StorageBackend storage, CSVFormat outputFormat) {
        this.storage = storage;
        this.outputFormat = outputFormat;
    }

    /**
     * Lagrer underlagsperiodene i underlaget som csv-rader vha {@link StorageBackend#lagre(Consumer)}.
     * @param underlag lagres som csv-rader
     * @see CSVFormat
     */
    public void lagreUnderlag(Stream<Underlag> underlag) {
        final String header = outputFormat.kolonnenavn().collect(joining(";"));
        lagre(header);
        underlag.map(u -> u
                .stream()
                .map(p -> outputFormat.serialiser(u, p))
                .map(s -> s.map(Object::toString))
                .map(s -> s.collect(joining(";")))
                .collect(joining("\n"))
        ).forEach(this::lagre);
    }

    private void lagre(String nyLinje) {
        storage.lagre(event -> event.buffer.append(nyLinje).append("\n"));
    }

}
