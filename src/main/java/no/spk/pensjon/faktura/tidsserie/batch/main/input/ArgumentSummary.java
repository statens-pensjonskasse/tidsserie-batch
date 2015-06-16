package no.spk.pensjon.faktura.tidsserie.batch.main.input;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;

/**
 * Util-klasse som bruker reflection for å lage en tekstlig oppsummering av innholdet i en klasse som
 * har {@link Parameter} annoteringer på felter i klassen.
 * @author Snorre E. Brekke - Computas
 */
public final class ArgumentSummary {

    private ArgumentSummary(){
    }

    /**
     * Lager en oppsummering i form av en streng som inneholder èn linje per {@link Parameter} felt i klassen.
     * <br>Følgende {@link Parameter} felt  blir utelatt fra oppsummeringen:
     * <ul>
     *     <li>
     *       Felt som har verdi lik {@link Optional#empty()}.
     *     </li>
     *     <li>
     *       Felt som annotert med {@link Parameter#help()}
     *     </li>
     * </ul>
     * @param programArguments instans av en klasse som har en eller flere {@link Parameter} annoteringer på felter
     * @return en streng som inneholder èn linje per {@link Parameter} felt
     */
    public static String createParameterSummary(Object programArguments) {
        return Arrays.stream(programArguments.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Parameter.class))
                .filter(f -> !f.getAnnotation(Parameter.class).help())
                .map(f -> summary(f, programArguments))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));
    }

    private static Optional<String >summary(Field field, Object instance) {
        Optional<?> value = getValue(field, instance);
        if (value.isPresent()) {
            Parameter annotation = field.getAnnotation(Parameter.class);
            String parameterString = annotation.names()[0] + ": ";
            parameterString += value.get();
            return Optional.of(parameterString);
        }
        return Optional.empty();
    }

    private static Optional<?> getValue(Field field, Object instance){
        try {
            return transformType(field.get(instance));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<?> transformType(Object fieldValue) {
        if (fieldValue instanceof Optional) {
            return (Optional)fieldValue;
        }
        return Optional.ofNullable(fieldValue);
    }
}
