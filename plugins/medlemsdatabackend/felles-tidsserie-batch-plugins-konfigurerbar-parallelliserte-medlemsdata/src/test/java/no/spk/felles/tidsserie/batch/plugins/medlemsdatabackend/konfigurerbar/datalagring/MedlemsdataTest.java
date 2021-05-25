package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MedlemsdataTest {

    @Test
    public void test_put_data_default() {
        Medlemsdata medlemsdata = DefaultMedlemsdata.medlemsdata("heisann".getBytes());

        assertEquals("heisann", new String(medlemsdata.medlemsdata()));

        medlemsdata.put(DefaultMedlemsdata.medlemsdata("test".getBytes()));

        assertEquals("heisann\ntest", new String(medlemsdata.medlemsdata()));
    }

    @Test
    public void test_put_data_skalert_buffer() {
        Medlemsdata medlemsdata = SkalertBufferMedlemsdata.medlemsdata("heisann".getBytes());

        assertEquals("heisann", new String(medlemsdata.medlemsdata()));

        medlemsdata.put(SkalertBufferMedlemsdata.medlemsdata("test".getBytes()));

        assertEquals("heisann\ntest", new String(medlemsdata.medlemsdata()));
    }

    @Test
    public void test_put_data_komprimert() {
        Medlemsdata medlemsdata = KomprimertMedlemsdata.medlemsdata("heisann".getBytes());

        assertEquals("heisann", new String(medlemsdata.medlemsdata()));

        medlemsdata.put(KomprimertMedlemsdata.medlemsdata("test".getBytes()));

        assertEquals("heisann\ntest", new String(medlemsdata.medlemsdata()));
    }
}