package no.spk.pensjon.faktura.tidsserie.plugin.modus.avregning_tidsserie;

import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Aarsverk;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Kroner;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Prosent;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsLengdeRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Aarsfaktor;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsfaktorRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AarsverkRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.AntallDagarRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.DeltidsjustertLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErMedregningRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErPermisjonUtanLoennRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.ErUnderMinstegrensaRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.LoennstilleggRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MaskineltGrunnlagRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MedregningsRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Minstegrense;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.MinstegrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.OevreLoennsgrenseRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.BegrunnetFaktureringsandel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.BegrunnetGruppelivsfaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.BegrunnetYrkesskadefaktureringRegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.FakturerbareDagsverk;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.FakturerbareDagsverkGRURegel;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.forsikringsprodukt.FakturerbareDagsverkYSKRegel;
import no.spk.felles.tidsperiode.AntallDagar;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Beregningsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.PaakrevdAnnotasjonManglarException;

/**
 * {@link FalskeReglar} implementerer falske versjonar av alle reglane som ikkje er spesifikke for avregning.
 * <br>
 * Intensjonen med dette er å redusere oppsettet som er påkrevd for å verifisere formatering og mapping
 * av tidsserien. Desse reglane blir testa i domenemodellen og av andre integrasjons- og akkseptansetestar. Det
 * er derfor ikkje påkrevd å teste og sette opp desse for avregninga.
 *
 * @author Tarjei Skorgenes
 */
class FalskeReglar {
    static AarsverkRegel aarsverkRegel(final double verdi) {
        return new AarsverkRegel() {
            @Override
            public Aarsverk beregn(final Beregningsperiode<?> periode) {
                return new Aarsverk(new Prosent(verdi));
            }
        };
    }

    static AarsfaktorRegel aarsfaktorRegel(final double verdi) {
        return new AarsfaktorRegel() {
            @Override
            public Aarsfaktor beregn(final Beregningsperiode<?> periode) {
                return new Aarsfaktor(verdi);
            }
        };
    }

    static AarsLengdeRegel aarslengdeRegel(final AntallDagar dagar) {
        return new AarsLengdeRegel() {
            @Override
            public AntallDagar beregn(final Beregningsperiode<?> periode) {
                return dagar;
            }
        };
    }

    static AntallDagarRegel antallDagarRegel(final AntallDagar dagar) {
        return new AntallDagarRegel() {
            @Override
            public AntallDagar beregn(final Beregningsperiode<?> periode) {
                return dagar;
            }
        };
    }

    static DeltidsjustertLoennRegel deltidsjustertLoennRegel(final Kroner beloep) {
        return new DeltidsjustertLoennRegel() {
            @Override
            public Kroner beregn(final Beregningsperiode<?> periode) throws PaakrevdAnnotasjonManglarException {
                return beloep;
            }
        };
    }

    static LoennstilleggRegel loennstilleggRegel(final Kroner beloep) {
        return new LoennstilleggRegel() {
            @Override
            public Kroner beregn(final Beregningsperiode<?> periode) {
                return beloep;
            }
        };
    }

    static MaskineltGrunnlagRegel maskineltGrunnlagRegel(final Kroner beloep) {
        return new MaskineltGrunnlagRegel() {
            @Override
            public Kroner beregn(final Beregningsperiode<?> periode) {
                return beloep;
            }
        };
    }

    static OevreLoennsgrenseRegel oevreLoennsgrenseRegel(final Kroner beloep) {
        return new OevreLoennsgrenseRegel() {
            @Override
            public Kroner beregn(final Beregningsperiode<?> periode) {
                return beloep;
            }
        };
    }

    static MedregningsRegel medregningsRegel(final Kroner beloep) {
        return new MedregningsRegel() {
            @Override
            public Kroner beregn(final Beregningsperiode<?> periode) {
                return beloep;
            }
        };
    }

    static MinstegrenseRegel minstegrenseRegel(final Minstegrense minstegrense) {
        return new MinstegrenseRegel() {
            @Override
            public Minstegrense beregn(final Beregningsperiode<?> periode) {
                return minstegrense;
            }
        };
    }

    static BegrunnetYrkesskadefaktureringRegel yrkesskadeFaktureringRegel(final BegrunnetFaktureringsandel status) {
        return new BegrunnetYrkesskadefaktureringRegel() {
            @Override
            public BegrunnetFaktureringsandel beregn(final Beregningsperiode<?> periode) {
                return status;
            }
        };
    }

    static BegrunnetGruppelivsfaktureringRegel gruppelivsfaktureringRegel(final BegrunnetFaktureringsandel status) {
        return new BegrunnetGruppelivsfaktureringRegel() {
            @Override
            public BegrunnetFaktureringsandel beregn(final Beregningsperiode<?> periode) {
                return status;
            }
        };
    }

    static ErUnderMinstegrensaRegel erUnderMinstegrensaRegel(final boolean resultat) {
        return new ErUnderMinstegrensaRegel() {
            @Override
            public Boolean beregn(final Beregningsperiode<?> periode) {
                return resultat;
            }
        };
    }

    static ErPermisjonUtanLoennRegel erPermisjonUtanLoenn(final boolean resultat) {
        return new ErPermisjonUtanLoennRegel() {
            @Override
            public Boolean beregn(final Beregningsperiode<?> periode) {
                return resultat;
            }
        };
    }

    static ErMedregningRegel erMedregningRegel(final boolean resultat) {
        return new ErMedregningRegel() {
            @Override
            public Boolean beregn(final Beregningsperiode<?> periode) {
                return resultat;
            }
        };
    }

    static FakturerbareDagsverkYSKRegel fakturerbareDagsverkYSKRegel(final FakturerbareDagsverk dagsverk) {
        return new FakturerbareDagsverkYSKRegel() {
            @Override
            public FakturerbareDagsverk beregn(final Beregningsperiode<?> periode) {
                return dagsverk;
            }
        };
    }

    static FakturerbareDagsverkGRURegel fakturerbareDagsverkGRURegel(final FakturerbareDagsverk dagsverk) {
        return new FakturerbareDagsverkGRURegel() {
            @Override
            public FakturerbareDagsverk beregn(final Beregningsperiode<?> periode) {
                return dagsverk;
            }
        };
    }
}
