package no.spk.pensjon.faktura.tidsserie.batch.main;

import static java.time.LocalDateTime.now;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast.FileTemplate;
import no.spk.pensjon.faktura.tidsserie.domain.tidsperiode.Aarstall;
import no.spk.pensjon.faktura.tidsserie.storage.main.CleanBatchError;
import no.spk.pensjon.faktura.tidsserie.storage.main.GrunnlagsdataException;
import no.spk.pensjon.faktura.tidsserie.storage.main.Oppryddingsstatus;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ArgumentSummary;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArguments;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory;
import no.spk.pensjon.faktura.tidsserie.storage.main.input.ProgramArgumentsFactory.UsageRequestedException;

/**
 * TODO: Kva og korleis �nskjer vi � vise status for batchk�yringa n�r vi k�yrer den for v�r egen bruk?
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

    public void startarTidsseriegenerering(FileTemplate malFilnavn, Aarstall fraOgMed, Aarstall tilOgMed) {
        println("Starter tidsserie-generering for �rsintervall fra og med " + fraOgMed +  " til og med " + tilOgMed);
    }

    /**
     * Viser informasjon om kva kommandolinjeargument batchen st�ttar med forklaring av
     * kva kvart argument blir brukt til og kva verdiar det st�ttar.
     *
     * @param e hjelp-foresp�rslen som inneheld informasjon om tilgjengelige argument
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
        println("F�lgende programargumenter blir brukt: ");
        println(ArgumentSummary.createParameterSummary(arguments));
    }

    @Override
    public void informerOmUslettbareArbeidskatalogar(Oppryddingsstatus status) {
        println("F�lgende kataloger kunne ikke slettes:\n" +
                status.getErrors()
                        .stream()
                        .map(CleanBatchError::getLabel)
                        .collect(Collectors.joining("\n")));
    }

    @Override
    public void informerOmSuksess(Path arbeidskatalog) {
        println("Resultat av kj�ringen ligger i katalogen " + arbeidskatalog);
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
        println("Grunnlagsdata i inn-katalogen er korrupte - avbryter kj�ringen.");
        println("�rsak: " + e.getMessage());
    }

    @Override
    public void tidsseriegenereringFullfoert(Map<String, Integer> meldingar) {
        println("Tidsseriegenerering fullf�rt.");
    }

    private void println(final String melding) {
        System.out.println(melding);
    }


}
