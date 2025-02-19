package no.spk.premie.tidsserie.batch.main;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;

class ManglandeServiceLoaderOppsettError extends Error {
    private static final long serialVersionUID = 0L;
    private final Class<?> type;

    ManglandeServiceLoaderOppsettError(final Class<?> type) {
        this.type = requireNonNull(type, "type er påkrevd, men var null");
    }

    @Override
    public String getMessage() {
        return join(
                "\n",
                format(
                        "Batchen klarte ikkje å opprette ei teneste av type %s",
                        type.getSimpleName()
                ),
                "",
                "Batchen benyttar ServiceLoader APIen for å opprette tenester i første fase av oppstarten.",
                "",
                "Denne APIen forventar å finne navnet på tenestas implementasjon på følgjande sti i batchens JAR-fil:",
                "",
                format(
                        "\t/META-INF/services/%s",

                        type.getName()
                ),
                "",
                "Denne fila ser ut til å mangle i batchen si JAR-fil.",
                "",
                "Vennligst undersøk kvifor fila manglar i batchen si JAR-fil og prøv å finne ut kva modul som skulle ha satt opp denne tenesta."
        );
    }
}

