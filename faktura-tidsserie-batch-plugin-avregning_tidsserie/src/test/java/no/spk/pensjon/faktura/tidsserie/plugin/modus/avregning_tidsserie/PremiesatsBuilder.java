package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie;

import static java.lang.Integer.parseInt;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner.kroner;
import static no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent.prosent;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Satser;

/**
 * @author riv
 */
public class PremiesatsBuilder {
    private String administrasjonsgebyr;
    private String medlem;
    private String arbeidsgiver;

    public static PremiesatsBuilder premiesatsBuilder() {
        return new PremiesatsBuilder();
    }

    public Satser<Prosent> prosentsatser() {
        return new Satser<>(prosent(arbeidsgiver), prosent(medlem), prosent(administrasjonsgebyr));
    }

    public Satser<Kroner> kronesatser() {
        return new Satser<>(kroner(parseInt(arbeidsgiver)), kroner(parseInt(medlem)), kroner(parseInt(administrasjonsgebyr)));
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
}
