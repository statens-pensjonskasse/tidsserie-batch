package no.spk.pensjon.faktura.tidsserie.batch.it;

public class ModusBeskrivelse {
    private final String navn;

    public ModusBeskrivelse(final String navn) {
        this.navn = navn;
    }

    public String navn() {
        return navn;
    }
}
