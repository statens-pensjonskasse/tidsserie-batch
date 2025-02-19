package no.spk.felles.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class SkalertBufferMedlemsdata implements Medlemsdata {
    private static final byte[] DELIMITER_ROW_BYTES = "\n".getBytes(StandardCharsets.UTF_8);
    private static final double VEKST_FAKTOR = 1.1;

    private byte[] medlemsdata;
    private int antallBytesLagra;

    private SkalertBufferMedlemsdata(final byte[] medlemsdata) {
        this.medlemsdata = medlemsdata;
        antallBytesLagra = medlemsdata.length;
    }

    public static Medlemsdata medlemsdata(final byte[] medlemsdata) {
        return new SkalertBufferMedlemsdata(medlemsdata);
    }

    public byte[] medlemsdata() {
        return medlemsdata.length == antallBytesLagra ?
                medlemsdata :
                Arrays.copyOfRange(medlemsdata, 0, antallBytesLagra);
    }

    public Medlemsdata put(final Medlemsdata innData) {
        if (ikkeNokPlassIArray(innData)) {
            skalerArrayMedVekstFaktor(antallBytesLagra + DELIMITER_ROW_BYTES.length + innData.medlemsdata().length);
        }

        kopier(DELIMITER_ROW_BYTES, medlemsdata, antallBytesLagra, DELIMITER_ROW_BYTES.length);
        kopier(innData.medlemsdata(), medlemsdata, antallBytesLagra +DELIMITER_ROW_BYTES.length, innData.medlemsdata().length);
        antallBytesLagra += DELIMITER_ROW_BYTES.length + innData.medlemsdata().length;

        return this;
    }

    private boolean ikkeNokPlassIArray(final Medlemsdata innData) {
        return antallBytesLagra + DELIMITER_ROW_BYTES.length + innData.medlemsdata().length > medlemsdata.length;
    }

    private void skalerArrayMedVekstFaktor(final int nyttLengdeBehov) {
        int nyLengde = medlemsdata.length == 0 ? nyttLengdeBehov : medlemsdata.length;
        while (nyLengde < nyttLengdeBehov) {
            nyLengde = (int) Math.ceil(nyLengde * VEKST_FAKTOR);
        }

        byte[] nyArray = new byte[nyLengde];

        kopier(medlemsdata, nyArray, 0, antallBytesLagra);
        medlemsdata = nyArray;
    }

    private void kopier(final byte[] fra, final byte[] til, final int tilIndex, final int lengde) {
        System.arraycopy(fra, 0, til, tilIndex, lengde);
    }
}
