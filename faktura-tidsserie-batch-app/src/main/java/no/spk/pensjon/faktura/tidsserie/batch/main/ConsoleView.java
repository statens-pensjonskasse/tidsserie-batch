package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.time.LocalDateTime.now;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import no.spk.faktura.input.ArgumentSummary;
import no.spk.faktura.input.InvalidParameterException;
import no.spk.faktura.input.UsageRequestedException;
import no.spk.pensjon.faktura.tidsserie.batch.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.batch.upload.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;

/**
 * TODO: Kva og korleis ønskjer vi å vise status for batchkøyringa når vi køyrer den for vår egen bruk?
 */
public class ConsoleView implements View{
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss");

    public void startarBackend() {
        println("Starter server.");
    }

    public void startarOpplasting() {
        println("Starter lasting av grunnlagsdata...");
    }

    public void opplastingFullfoert() {
        println("Grunnlagsdata lastet.");
}

    public void startarTidsseriegenerering(Aarstall fraOgMed, Aarstall tilOgMed) {
        println("Starter tidsserie-generering for årsintervall fra og med " + fraOgMed +  " til og med " + tilOgMed);
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
    public void informerOmUgyldigKommandolinjeArgument(InvalidParameterException e){
        println(e.getMessage());
        println(e.usage());
    }

    public void informerOmOppstart(ProgramArguments arguments) {
        println("Tidsserie-batch startet " + now().format(DATE_TIME_FORMATTER));
        println("Følgende programargumenter blir brukt: ");
        println(ArgumentSummary.createParameterSummary(arguments));
        arguments.postMessage().ifPresent(this::println);
    }


    @Override
    public void informerOmSuksess(Path arbeidskatalog) {
        println("Resultat av kjøringen ligger i katalogen " + arbeidskatalog);
        println("Tidsserie-batch avsluttet OK " + now().format(DATE_TIME_FORMATTER));
    }

    @Override
    public void informerOmUkjentFeil() {
        println("Tidsserie-batch feilet - se logfil for detaljer.");
    }

    @Override
    public void informerOmOppryddingStartet() {
        println("Sletter gamle filer.");
    }

    @Override
    public void informerOmKorrupteGrunnlagsdata(GrunnlagsdataException e) {
        println("Grunnlagsdata i inn-katalogen er korrupte - avbryter kjøringen.");
        println("Årsak: " + e.getMessage());
    }

    @Override
    public void tidsseriegenereringFullfoert(Map<String, Integer> meldingar) {
        println("Tidsseriegenerering fullført.");
    }

    @Override
    public void informerOmGrunnlagsdataValidering() {
        println("Validerer grunnlagsdata.");
    }

    @Override
    public void informerOmMetadataOppretting() {
        println("Oppretter metadata.");
    }

    @Override
    public void informerOmFeiletOpprydding() {
        println("Klarte ikke å rydde opp i ut- og/eller log-kataloger.");
        println("Se loggen for årsak.");
    }

    @Override
    public void informerOmTimeout() {
        println("Timeout - batchen har brukt for lang tid på å kjøre, og vil bli avsluttet.");
    }

    private void println(final String melding) {
        System.out.println(melding);
    }


}
