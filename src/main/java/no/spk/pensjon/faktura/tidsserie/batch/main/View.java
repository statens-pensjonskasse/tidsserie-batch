package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.time.LocalDateTime.now;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ArgumentSummary;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.UsageRequestedException;

/**
 * TODO: Kva og korleis ønskjer vi å vise status for batchkøyringa når vi køyrer den for vår egen bruk?
 */
public class View {
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss");

    public void startarBackend() {
    }

    public void startarOpplasting() {
    }

    public void opplastingFullfoert() {
    }

    public void startarTidsseriegenerering(FileTemplate malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) {
    }

    public void tidsseriegenereringFullfoert(Map<String, Integer> meldingar) {
    }

    public void fatalFeil(Exception e) {
    }

    public void ryddarOppFilerFraaTidligereKjoeringer() {
    }

    public void verifisererInput() {
    }

    /**
     * Viser informasjon om kva kommandolinjeargument batchen støttar med forklaring av
     * kva kvart argument blir brukt til og kva verdiar det støttar.
     *
     * @param e hjelp-forespørslen som inneheld informasjon om tilgjengelige argument
     */
    public void visHjelp(UsageRequestedException e){
        println(e.usage());
    }

    /**
     * Notifiserer brukaren om at eit av kommandolinjeargumenta som er angitt, er ugyldig med
     * informasjon om kva som er feil.
     *
     * @param e valideringsfeilen som inneheld informasjon om kva som er feil med argumentet
     */
    public void informerOmUgyldigKommandolinjeArgument(ProgramArgumentsFactory.InvalidParameterException e){
        println(e.getMessage());
        println(e.usage());
    }

    public void informerOmOppstart(ProgramArguments arguments) {
        println("Grunnlagsdata-batch startet " + now().format(DATE_TIME_FORMATTER));
        println("Følgende programargumenter blir brukt: ");
        println(ArgumentSummary.createParameterSummary(arguments));
    }

    private void println(final String melding) {
        System.out.println(melding);
    }
}
