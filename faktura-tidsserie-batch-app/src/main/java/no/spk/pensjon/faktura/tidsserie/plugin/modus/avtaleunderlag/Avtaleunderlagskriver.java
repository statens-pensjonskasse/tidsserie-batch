package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static java.util.stream.Collectors.joining;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.core.CSVFormat;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;

/**
 * Har i oppgave å skrive resultatet av {@link AvtaleunderlagFactory} matet inn til
 * {@link Avtaleunderlagformat} som csv-rader til fil. Fil-navnet bestemmes av {@link FileTemplate#createUniqueFile(long)}.
 * @author Snorre E. Brekke - Computas
 */
public class Avtaleunderlagskriver {

    private static final int SERIENUMMER = 1;
    private final FileTemplate fileTemplate;
    private final CSVFormat outputFormat;

    public Avtaleunderlagskriver(FileTemplate fileTemplate, CSVFormat outputFormat) {
        this.fileTemplate = fileTemplate;
        this.outputFormat = outputFormat;
    }

    public void skrivAvtaleunderlag(Stream<Underlag> underlag) {
        final File file = fileTemplate.createUniqueFile(SERIENUMMER);
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            final String header = outputFormat.kolonnenavn().collect(joining(";"));
            skriv(writer, header);
            underlag.map(u -> u
                    .stream()
                    .map(p -> outputFormat.serialiser(u, p))
                    .map(s -> s.map(Object::toString))
                    .map(s -> s.collect(joining(";")))
                    .collect(joining("\n"))
            ).forEach(s -> skriv(writer, s));
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void skriv(BufferedWriter writer, String nyLinje) {
        try {
            writer.write(nyLinje);
            writer.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
