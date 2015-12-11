package no.spk.pensjon.faktura.tidsserie.plugin.modus.underlagsperioder;

import java.time.LocalDate;
import java.util.Optional;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;

/**
 * {@link Kolonnetyper} inneheld støttefunksjonar for serialisering av
 * dei forskjellige verditypene som kontrakta for DVH-formatet støttar.
 *
 * @author Tarjei Skorgenes
 */
class Kolonnetyper {
    static String dato(final LocalDate verdi) {
        return verdi.toString();
    }

    static String heiltall(final int verdi) {
        return Integer.toString(verdi);
    }

    static String heiltall(final long verdi) {
        return Long.toString(verdi);
    }

    static String flagg(final boolean value) {
        return value ? "1" : "0";
    }


    static String flagg(final Optional<Boolean> value) {
        return value.map(Kolonnetyper::flagg).orElse("");
    }

    static String kode(final String verdi) {
        return verdi;
    }

    static String kode(final Optional<?> verdi) {
        return verdi.map(Object::toString).orElse("");
    }

    static String beloep(final Optional<Kroner> verdi) {
        return verdi.map(Kolonnetyper::beloep).orElse("");
    }

    static String beloep(final Kroner beloep) {
        return Long.toString(beloep.verdi());
    }
}
