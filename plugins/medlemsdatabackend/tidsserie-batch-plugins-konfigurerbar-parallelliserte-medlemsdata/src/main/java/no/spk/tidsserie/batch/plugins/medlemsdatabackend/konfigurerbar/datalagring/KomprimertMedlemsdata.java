package no.spk.tidsserie.batch.plugins.medlemsdatabackend.konfigurerbar.datalagring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

class KomprimertMedlemsdata implements Medlemsdata {
    private static final String DELIMITER_ROW = "\n";
    private static final byte[] DELIMITER_ROW_BYTES = DELIMITER_ROW.getBytes(StandardCharsets.UTF_8);

    private byte[] medlemsdata;

    private KomprimertMedlemsdata(final byte[] medlemsdata) {
        komprimerMedlemsdata(medlemsdata);
    }

    public static Medlemsdata medlemsdata(final byte[] medlemsdata) {
        return new KomprimertMedlemsdata(medlemsdata);
    }

    public byte[] medlemsdata() {
        return dekomprimerMedlemsdata();
    }

    public Medlemsdata put(final Medlemsdata innData) {
        komprimerMedlemsdata(this.medlemsdata(), DELIMITER_ROW_BYTES, innData.medlemsdata());
        return this;
    }

    private void komprimerMedlemsdata(final byte[]... medlemsdataer) {
        try {
            ByteArrayOutputStream resultat = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(resultat);
            for (byte[] medlemsdata : medlemsdataer) {
                deflater.write(medlemsdata);
            }
            deflater.flush();
            deflater.close();

            this.medlemsdata = resultat.toByteArray();
        } catch (IOException e) {
            throw new KlarteIkkeKomprimereMedlemsdataException(e.getMessage());
        }
    }

    private byte[] dekomprimerMedlemsdata() {
        try {
            ByteArrayOutputStream resultat = new ByteArrayOutputStream();
            InflaterOutputStream inflater = new InflaterOutputStream(resultat);
            inflater.write(this.medlemsdata);
            inflater.flush();
            inflater.close();

            return resultat.toByteArray();
        } catch (IOException e) {
            throw new KlarteIkkeDekomprimereMedlemsdataException(e.getMessage());
        }
    }
}
