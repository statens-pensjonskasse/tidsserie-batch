package no.spk.pensjon.faktura.tidsserie.storage.csv;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * StringListToObjectFactory transformerer lister av strenger til instanser av en angitt klasse.
 * <p>
 * Ved konstruksjon finner StringListToObjectFactory alle felt annotert med {@link CsvIndex} i en klasse og tilbyr metoden {@link #transform(List)} for å
 * gjøre om en liste av strenger til en instans av denne klassen.<br>
 *     Nb: P.t. støtter denne klassen kun felt av typen {@code Optional<String>}
 * </p>
 * <p>
 * Det er en forutsenting at lister med strenger har typeindikator angitt på index 0, og at metoden {@link #transform(List)} bare
 * brukes dersom {@link #supports(List)} returnerer {@code true}.
 * </p>
 * @param <T> Typen som skal strenglister skal transformeres til.
 * @author Snorre E. Brekke - Computas
 * @see CsvIndex
 */
public final class StringListToObjectFactory<T> {
    private final Class<T> csvClass;

    private final List<Field> csvFields;
    private final int columns;
    private final String type;

    private final OversetterSupport oversetterSupport = new OversetterSupport();

    /**
     * Lager en ny StringListToObjectFactory som kan transformere lister med strenger som har {@code type} angitt på index 0,
     * om til instanser av {@code csvClass}.<br>
     *     Nb: P.t. støtter denne klassen kun annoterte felt av typen {@code Optional<String>}
     * @param type Streng som angir typeindikator for lister som skal transformeres til {@code csvClass}.
     * @param csvClass Klassen til typen strenglister skal transformeres til.
     * @throws IllegalArgumentException dersom klassen ikke har noen felter annotert med {@link CsvIndex}
     */
    public StringListToObjectFactory(String type, Class<T> csvClass) {
        this.csvClass = Objects.requireNonNull(csvClass);
        this.type = Objects.requireNonNull(type);

        csvFields = findCsvFields();
        assertAllFieldsOptionalString();

        columns = csvFields.stream()
                .map(f -> f.getAnnotation(CsvIndex.class))
                .sorted((a1, a2) -> a2.value() - a1.value())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(csvClass.toString() + " hadde ingen felt annotert med @CsvIndex"))
                .value() + 1;
    }

    /**
     * Angir om raden inneholder en typeindikator på index 0 som er støttet av StringListToObjectFactory-instansen
     * @param rad som skal transformeres til T
     * @return true dersom randen har typeindikator støttet av {@code this} på index 0
     */
    public boolean supports(final List<String> rad) {
        return !rad.isEmpty() && type.equals(rad.get(0));
    }

    /**
     * Transformerer listen med strenger til T
     * @param rad som skal transformeres til T
     * @return rad transformert til T
     * @throws IllegalStateException dersom raden har for få kolonner eller et annotert felt ikke er av typen {@code Optional<String>}
     */
    public T transform(final List<String> rad) {
        try {
            if (rad.size() < columns) {
                throw new IllegalArgumentException(getErrorMessage(rad));
            }
            T newCsvInstance = csvClass.newInstance();
            csvFields.stream().forEach(f -> setValue(f, rad, newCsvInstance));
            return newCsvInstance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setValue(Field field, List<String> rad, T avtaleCsv) {
        try {
            field.set(avtaleCsv, read(rad, getIndex(field)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<String> read(final List<String> rad, final int index) {
        return oversetterSupport.read(rad, index);
    }

    private List<Field> findCsvFields() {
        return Arrays.stream(csvClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(CsvIndex.class))
                .sorted((f1, f2) -> getIndex(f1) - getIndex(f2))
                .collect(toList());
    }

    private int getIndex(Field f2) {
        return f2.getAnnotation(CsvIndex.class).value();
    }

    private void assertAllFieldsOptionalString() {
        csvFields.stream()
                .forEach(f -> {
                    if (!f.getGenericType().getTypeName().equals("java.util.Optional<java.lang.String>")) {
                        throw new IllegalStateException("Feltet '" + f.getName() + "' må ha typen Optional<String>. Fant: " + f.getType());
                    }

                });
    }

    private String getErrorMessage(List<String> rad) {
        return csvFields.stream()
                .sorted((f1, f2) -> getIndex(f1) - getIndex(f2))
                .filter(f -> getIndex(f) != 0)
                .map(f -> f.getName() + "(" + getIndex(f) + ")")
                .collect(Collectors.joining(", ",
                        "Rader av typen <" + type + "> må inneholde minimum <" + columns + "> kolonner, " +
                                "med følgende verdier på angitt index:\n typeindikator(0), ",
                        "\nRaden som feilet: " + rad));
    }

}
