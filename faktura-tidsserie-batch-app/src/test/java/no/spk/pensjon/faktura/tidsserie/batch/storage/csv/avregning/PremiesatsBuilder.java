package no.spk.pensjon.faktura.tidsserie.batch.storage.csv.avregning;

/**
 * @author riv
 */
public class PremiesatsBuilder {
    private String administrasjonsgebyr;
    private String medlem;
    private String arbeidsgiver;

    public static PremiesatsBuilder premiesatser() {
        return new PremiesatsBuilder();
    }

    public PremiesatsBuilder administrasjonsgebyr(final String administrasjonsgebyr) {
        this.administrasjonsgebyr = administrasjonsgebyr;
        return this;
    }

    public PremiesatsBuilder medlem(final String medlem) {
        this.medlem = medlem;
        return this;
    }

    public PremiesatsBuilder arbeidsgiver(final String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
        return this;
    }

    public String arbeidsgiver() {
        return this.arbeidsgiver;
    }

    public String medlem() {
        return this.medlem;
    }

    public String administrasjonsgebyr() {
        return this.administrasjonsgebyr;
    }
}
