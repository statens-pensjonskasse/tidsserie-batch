package no.spk.pensjon.faktura.tidsserie.plugin.modus.avtaleunderlag;

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.groupingBy;

import java.util.function.Function;
import java.util.stream.Stream;

import no.spk.pensjon.faktura.tidsserie.core.TidsperiodeFactory;
import no.spk.pensjon.faktura.tidsserie.core.Tidsserienummer;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Arbeidsgiverdataperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleprodukt;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtalerelatertperiode;
import no.spk.pensjon.faktura.tidsserie.domain.avtaledata.Avtaleversjon;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.ArbeidsgiverId;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.Avtale;
import no.spk.pensjon.faktura.tidsserie.domain.grunnlagsdata.AvtaleId;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelperiode;
import no.spk.pensjon.faktura.tidsserie.domain.reglar.Regelsett;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aar;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Tidsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.tidsserie.AvtaleFactory;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Observasjonsperiode;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlag;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.UnderlagFactory;
import no.spk.pensjon.faktura.tidsserie.domain.underlag.Underlagsperiode;

/**
 * Lager {@link Underlag} med {@link Underlagsperiode}'er basert på grunnlagsdata hentet fra {@link TidsperiodeFactory}.
 *
 * @author Snorre E. Brekke - Computas
 */
public class AvtaleunderlagFactory {
    private final TidsperiodeFactory grunnlagsdata;
    private final AvtaleFactory avtaleFactory;
    private final Regelsett regelsett;

    public AvtaleunderlagFactory(TidsperiodeFactory tidsperiodeFactory, Regelsett regelsett) {
        this.grunnlagsdata = tidsperiodeFactory;
        this.avtaleFactory = new AvtaleFactory();
        this.regelsett = regelsett;
    }

    /**
     * Lager et underlag som skal benyttes for å skrive ut avtaleperioder basert på grunnlagsdataene angitt ved konstruksjon.
     *
     * @param observasjonsperiode periode underlaget skal lages for
     * @param uttrekksdato dato for når grunnlagsdata ble hentet fra Kasper
     * @return en strøm med underlagene basert på grunnlagsdata
     */
    @SuppressWarnings({"unchecked"})
    public Stream<Underlag> lagAvtaleunderlag(Observasjonsperiode observasjonsperiode, Uttrekksdato uttrekksdato) {
        Tidsserienummer tidsserienummer = Tidsserienummer.genererForDato(now());

        return Stream.of(
                grunnlagsdata.perioderAvType(Avtaleperiode.class),
                grunnlagsdata.perioderAvType(Avtaleversjon.class),
                grunnlagsdata.perioderAvType(Avtaleprodukt.class)
        )
                .flatMap(Function.identity())
                .map(p -> (Avtalerelatertperiode<?>) p)
                .collect(
                        groupingBy(Avtalerelatertperiode::avtale))
                .entrySet()
                .stream()
                .map(e -> new UnderlagFactory(
                                observasjonsperiode
                        )
                                .addPerioder(
                                        observasjonsperiode.overlappendeAar().stream()
                                )
                                .addPerioder(
                                        regelsett.reglar()
                                )
                                .addPerioder(
                                        e.getValue()
                                                .stream()
                                                .map(p -> (Tidsperiode<?>) p)
                                )
                                .addPerioder(grunnlagsdata.perioderAvType(Arbeidsgiverdataperiode.class))
                                .periodiser()
                                .annoter(AvtaleId.class, e.getKey())
                )
                .map(u -> new Underlag(
                                u.stream().filter(this::erKobletTilAvtale)
                        )
                                .annoterFra(u)
                )
                .filter(u -> !u.toList().isEmpty())
                .peek(u -> u
                        .stream()
                        .peek(p -> p.koblingAvType(Aar.class).ifPresent(a -> p.annoter(Aarstall.class, a.aarstall())))
                        .peek(p -> p.annoter(Avtale.class, avtaleFactory.lagAvtale(p, u.annotasjonFor(AvtaleId.class))))
                        .peek(p -> p.koblingAvType(Avtaleversjon.class).ifPresent(av -> av.annoter(p)))
                        .peek(p -> p.koblingarAvType(Regelperiode.class).forEach(r -> r.annoter(p)))
                        .peek(p -> p.annoter(Uttrekksdato.class, uttrekksdato))
                        .peek(p -> p.annoter(Tidsserienummer.class, tidsserienummer))
                        .peek(this::annoterArbeidsgiverdata)
                        .count()
                );
    }

    private void annoterArbeidsgiverdata(Underlagsperiode periode) {
        periode.koblingAvType(Avtaleperiode.class).ifPresent(ap -> {
            final ArbeidsgiverId arbeidsgiverid = ap.arbeidsgiverId();
            periode.annoter(ArbeidsgiverId.class, arbeidsgiverid);

            periode.koblingarAvType(Arbeidsgiverdataperiode.class)
                    .filter(arbeidsgiverperiode -> arbeidsgiverperiode.tilhoeyrer(arbeidsgiverid))
                    .reduce((a, b) -> {
                        throw new IllegalStateException(periode +
                                " med arbeidsgiverid" + arbeidsgiverid +
                                " overlapper flere arbeidsperioder med samme arbeidsgiverid.");
                    })
                    .ifPresent(arbeidsgiverperiode -> arbeidsgiverperiode.annoter(periode));
        });
    }

    private boolean erKobletTilAvtale(Underlagsperiode p) {
        return p.koblingAvType(Avtaleperiode.class).isPresent() ||
                p.koblingAvType(Avtaleversjon.class).isPresent() ||
                p.koblingAvType(Avtaleprodukt.class).isPresent();
    }
}
