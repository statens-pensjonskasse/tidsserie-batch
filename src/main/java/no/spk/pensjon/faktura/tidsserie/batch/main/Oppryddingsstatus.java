package no.spk.pensjon.faktura.tidsserie.batch.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Snorre E. Brekke - Computas
 */
public class Oppryddingsstatus {
    private List<CleanBatchError> errors = new ArrayList<>();

    public void addError(String label, Exception exception) {
        errors.add(new CleanBatchError(label, exception));
    }

    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    public List<CleanBatchError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
