package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

import java.nio.charset.StandardCharsets;

class DefaultMedlemsdata implements Medlemsdata {
    private static final String DELIMITER_ROW = "\n";
    private static final byte[] DELIMITER_ROW_BYTES = DELIMITER_ROW.getBytes(StandardCharsets.UTF_8);

    private byte[] medlemsdata;

    private DefaultMedlemsdata(final byte[] medlemsdata) {
        this.medlemsdata = medlemsdata;
    }

    public static Medlemsdata medlemsdata(final byte[] medlemsdata) {
        return new DefaultMedlemsdata(medlemsdata);
    }

    public byte[] medlemsdata() {
        return medlemsdata;
    }

    public Medlemsdata put(final Medlemsdata innData) {
        byte[] innDataBytes = innData.medlemsdata();
        byte[] nyttArray = new byte[medlemsdata.length + DELIMITER_ROW_BYTES.length + innDataBytes.length];

        kopier(medlemsdata, nyttArray, 0);
        kopier(DELIMITER_ROW_BYTES, nyttArray, medlemsdata.length);
        kopier(innDataBytes, nyttArray, medlemsdata.length + DELIMITER_ROW_BYTES.length);

        this.medlemsdata = nyttArray;
        return this;
    }

    private void kopier(final byte[] fra, final byte[] til, final int tilIndex) {
        System.arraycopy(fra, 0, til, tilIndex, fra.length);
    }
}
