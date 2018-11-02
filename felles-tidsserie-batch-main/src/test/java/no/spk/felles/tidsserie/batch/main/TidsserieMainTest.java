package no.spk.felles.tidsserie.batch.main;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.Test;

public class TidsserieMainTest {
    @Test
    public void skal_gi_ei_forståelig_feilmelding_dersom_classpath_manglar_teneste_av_ønska_type() {
        assertThatCode(
                () -> TidsserieMain.finnFørsteTjeneste(Dummy.class)
        )
                .isInstanceOf(ManglandeServiceLoaderOppsettError.class)
                .hasMessageContaining("Batchen klarte ikkje å opprette ei teneste av type ")
                .hasMessageContaining(Dummy.class.getSimpleName())
                .hasMessageContaining("Batchen benyttar ServiceLoader APIen for å opprette tenester i første fase av oppstarten.")
                .hasMessageContaining("Denne APIen forventar å finne navnet på tenestas implementasjon på følgjande sti i batchens JAR-fil:")
                .hasMessageContaining("/META-INF/services/no.spk.felles.tidsserie.batch.main.TidsserieMainTest$Dummy")
                .hasMessageContaining("Denne fila ser ut til å mangle i batchen si JAR-fil.")
                .hasMessageContaining("Vennligst undersøk kvifor fila manglar i batchen si JAR-fil og prøv å finne ut kva modul som skulle ha satt opp denne tenesta.")
        ;
    }

    public static class Dummy {
    }
}