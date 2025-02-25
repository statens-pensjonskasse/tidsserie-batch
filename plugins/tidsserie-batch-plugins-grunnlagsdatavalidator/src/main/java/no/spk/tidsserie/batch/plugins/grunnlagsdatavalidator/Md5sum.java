package no.spk.tidsserie.batch.plugins.grunnlagsdatavalidator;

import static java.lang.Integer.toHexString;
import static java.nio.file.Files.readAllBytes;
import static java.security.MessageDigest.getInstance;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.NoSuchAlgorithmException;

public class Md5sum {
    public String produser(final File file) {
        try {
            return toHex(md5(read(file)));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String toHex(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (final byte b : bytes) {
            final String tekst = toHexString((int) b & 0xff);
            if (!(2 == tekst.length())) {
                sb.append('0');
            }
            sb.append(tekst);
        }
        return sb.toString();
    }

    private byte[] md5(final byte[] bytes) throws IOException {
        try {
            return getInstance("MD5").digest(bytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private byte[] read(final File file) throws IOException {
        return readAllBytes(file.toPath());
    }
}