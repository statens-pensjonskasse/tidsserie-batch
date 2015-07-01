package no.spk.pensjon.faktura.tidsserie.storage.csv;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Snorre E. Brekke - Computas
 */
public class StringListToObjectFactoryTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testRequiresOptionalStringField() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Feltet 'type' må ha typen Optional<String>");
        new StringListToObjectFactory<>("WrongType", WrongType.class);
    }

    @Test
    public void testRequiresStringGenericField() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Feltet 'type' må ha typen Optional<String>");
        new StringListToObjectFactory<>("WrongGenericType", WrongGenericType.class);
    }

    @Test
    public void testRequiresEnoughColumns() throws Exception {
        StringListToObjectFactory<ParsableType> factory = new StringListToObjectFactory<>("TYPE", ParsableType.class);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Rader av typen <TYPE> må inneholde minimum <4> kolonner, ");
        exception.expectMessage("med følgende verdier på angitt index:");
        exception.expectMessage("typeindikator(0), field1(1), field2(3)");
        factory.transform(Arrays.asList("TYPE", "123"));
    }

    @Test
    public void testParseAvtale() throws Exception {
        StringListToObjectFactory<ParsableType> factory = new StringListToObjectFactory<>("TYPE", ParsableType.class);

        ParsableType parsableType = factory.transform(Arrays.asList("TYPE", "field1", "ignored", "field2"));

        assertThat(parsableType.field1.get()).isEqualTo("field1");
        assertThat(parsableType.field2.get()).isEqualTo("field2");
    }

    @Test
    public void testStoetterLinjeMedRiktigTypeIFoersteKolonne() throws Exception {
        StringListToObjectFactory<ParsableType> factory = new StringListToObjectFactory<>("TYPE", ParsableType.class);
        boolean supports = factory.supports(Arrays.asList("TYPE", "field1", "ignored", "field2"));
        assertThat(supports).isTrue();
    }

    @Test
    public void testStoetterIkkeLinjeMedFeilTypeIFoersteKolonne() throws Exception {
        StringListToObjectFactory<ParsableType> factory = new StringListToObjectFactory<>("TYPE", ParsableType.class);
        boolean supports = factory.supports(Arrays.asList("SOME_OTHER_TYPE", "field1", "ignored", "field2"));
        assertThat(supports).isFalse();
    }

    private static class ParsableType{
        ParsableType(){}
        @CsvIndex(1)
        Optional<String> field1;
        @CsvIndex(3)
        Optional<String> field2;
    }

    private static class WrongType{
        @CsvIndex(0)
        String type;
    }

    private static class WrongGenericType{
        @CsvIndex(0)
        Optional<Object> type;
    }
}