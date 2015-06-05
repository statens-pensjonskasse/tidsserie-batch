package no.spk.pensjon.faktura.tidsserie.storage.main.input;

import static java.lang.Integer.parseInt;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import no.spk.pensjon.faktura.grunnlagsdata.batch.BatchId;
import no.spk.pensjon.faktura.grunnlagsdata.batch.BatchParameters;
import no.spk.pensjon.faktura.grunnlagsdata.batch.timeout.BatchTimeout;
import no.spk.pensjon.faktura.grunnlagsdata.batch.timeout.TimeProvider;

/**
 * Transformerer ProgramArguments til BatchParameters.
 * @author Snorre E. Brekke - Computas
 * @see ProgramArguments
 * @see BatchParameters
 */
public class ProgramArgumentsToBatchParameters {
    private final TimeProvider timeProvider;

    public ProgramArgumentsToBatchParameters(final TimeProvider timeProvider) {
        this.timeProvider = requireNonNull(timeProvider);
    }

    /**
     * Transformerer ProgramArguments til BatchParameters.
     * @param args programArguments typisk populert av JCommander.
     * @param id identifikator for køyringa av batchen som skal til å bli gjennomført
     * @return BachParameters basert på ProgramArguments
     */
    public BatchParameters create(final ProgramArguments args, final BatchId id) {
        BatchParameters parameters = new BatchParameters();
        parameters.setBatchId(id);
        parameters.setArbeidskatalog(id.tilArbeidskatalog(args.getUtkatalog()).toString());
        parameters.setBeskrivelse(args.getBeskrivelse());
        parameters.setFraDato(args.getFraAar() + ".01.01");
        parameters.setTilDato(args.getTilAar() + ".12.31");
        parameters.setFraAvtale(args.getFraAvtale());
        parameters.setTilAvtale(args.getTilAvtale());
        parameters.setChunkSize(args.getChunkSize());
        parameters.setBatchTimeout(createBatchTimeout(args));
        parameters.setUrl(args.getJdbcUrl());
        parameters.setUsername(args.getJdbcUsername());
        parameters.setPasswordFile(args.getJdbcPassword());
        return parameters;
    }

    private BatchTimeout createBatchTimeout(ProgramArguments args) {
        String kjoeretidString = args.getKjoeretid();
        int hours = parseInt(kjoeretidString.substring(0, 2));
        int minutes = parseInt(kjoeretidString.substring(2, 4));
        Duration timeout = Duration.of(hours, HOURS).plus(Duration.of(minutes, MINUTES));
        return new BatchTimeout(timeout, args.getSluttidspunkt(), timeProvider);
    }
}
