package no.spk.felles.tidsserie.batch.main;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import no.spk.felles.tidsserie.batch.core.grunnlagsdata.UgyldigUttrekkException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.BruksveiledningSkalVisesException;
import no.spk.felles.tidsserie.batch.core.kommandolinje.TidsserieBatchArgumenter;
import no.spk.felles.tidsserie.batch.core.kommandolinje.UgyldigKommandolinjeArgumentException;

import org.slf4j.Logger;

public class ConsoleView implements View{
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss");

    private final Optional<Logger> logger;

    private Supplier<LocalDateTime> klokke = LocalDateTime::now;

    public ConsoleView() {
        logger = empty();
    }

    ConsoleView(final Logger logger) {
        requireNonNull(logger, "logger kan ikke være null.");
        this.logger = of(logger);
    }

    @Override
    public void startarBackend() {
        println("Starter server.");
    }

    @Override
    public void startarOpplasting() {
        println("Starter lasting av grunnlagsdata...");
    }

    @Override
    public void opplastingFullfoert() {
        println("Grunnlagsdata lastet.");
}

    @Override
    public void startarTidsseriegenerering() {
        println("Starter tidsserie-generering");
    }

    /**
     * Viser informasjon om kva kommandolinjeargument batchen støttar med forklaring av
     * kva kvart argument blir brukt til og kva verdiar det støttar.
     *
     * @param e hjelp-forespørslen som inneheld informasjon om tilgjengelige argument
     */
    @Override
    public void visHjelp(BruksveiledningSkalVisesException e){
        println(e.bruksveiledning());
    }

    /**
     * Notifiserer brukaren om at eit av kommandolinjeargumenta som er angitt, er ugyldig med
     * informasjon om kva som er feil.
     *
     * @param e valideringsfeilen som inneheld informasjon om kva som er feil med argumentet
     */
    @Override
    public void informerOmUgyldigKommandolinjeArgument(UgyldigKommandolinjeArgumentException e){
        println(e.getMessage());
        println(e.bruksveiledning());
    }

    @Override
    public void informerOmOppstart(final TidsserieBatchArgumenter arguments) {
        println("Tidsserie-batch startet " + now());
        println("Følgende programargumenter blir brukt: ");
        println(arguments.toString());
    }

    @Override
    public void informerOmSuksess(Path arbeidskatalog) {
        println("Resultat av kjøringen ligger i katalogen " + arbeidskatalog);
        println("Tidsserie-batch avsluttet OK " + now());
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
    public void informerOmKorrupteGrunnlagsdata(UgyldigUttrekkException e) {
        println("Grunnlagsdata i inn-katalogen er korrupte - avbryter kjøringen.");
        println("Årsak: " + e.getMessage());
    }

    @Override
    public void tidsseriegenereringFullfoert(Map<String, Integer> meldingar, String modusnavn) {
        println("Tidsseriegenerering fullført.");
        printMeldinger(meldingar, requireNonNull(modusnavn, "modusnavnet er påkrevd, men var null"));
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
        logger.ifPresent(l -> l.info(melding));
        System.out.println(melding);
    }

    private void printMeldinger(Map<String, Integer> meldinger, String modusnavn) {
        Map<String, Integer> sorterteMeldinger = sortereMeldinger(meldinger);
        System.out.println(lageModusmelding(sorterteMeldinger, modusnavn));

        Integer antallFeil = sorterteMeldinger.entrySet()
                .stream()
                .filter(map ->  "errors".equals(map.getKey()))
                .map(Map.Entry::getValue)
                .reduce(0, Integer::sum);
        System.out.println("Antall feil: " + antallFeil);

        for(Map.Entry<String, Integer> entry : sorterteMeldinger.entrySet()) {
            logger.ifPresent(l -> l.info(entry.toString()));
        }
    }

    private static Map<String, Integer> sortereMeldinger(Map<String, Integer> meldinger) {
        return meldinger.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e2, LinkedHashMap::new));
    }

    private String lageModusmelding(Map<String, Integer> meldinger, String modusnavn) {
        String avtaleunderlag = "Antall avtaler behandlet: " + meldinger.get("avtaler");
        String andremoduser = "Antall medlemmer behandlet: " + meldinger.get("medlem");
        return modusnavn.equals("avtaleunderlag") ? avtaleunderlag : andremoduser;
    }

    private String now() {
        return klokke.get().format(DATE_TIME_FORMATTER);
    }

    ConsoleView overstyr(final Supplier<LocalDateTime> klokke) {
        this.klokke = requireNonNull(klokke, "klokke er påkrevd, men var null");
        return this;
    }
}
