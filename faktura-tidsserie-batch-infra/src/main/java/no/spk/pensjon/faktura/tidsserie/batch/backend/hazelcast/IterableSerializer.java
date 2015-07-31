package no.spk.pensjon.faktura.tidsserie.batch.backend.hazelcast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

class IterableSerializer implements StreamSerializer<List<List<String>>> {

    @Override
    public int getTypeId() {
        return 1;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void write(final ObjectDataOutput out, final List<List<String>> rows) throws IOException {
        final int rowCount = rows.size();
        if (rowCount > 32767) {
            throw new Error("Serialisering av medlem med " + rowCount + " endringar feila, øvre antall endringar som er støtta er 32767, endringar = " + rows);
        }
        out.writeShort(rowCount);

        for (final List<String> row : rows) {
            final int columnCount = row.size();
            if (columnCount > 127) {
                throw new Error("Serialisering av endring med " + columnCount + " kolonner feila, øvre grense for antall kolonner pr endring er 127 kolonner, rad = '" + row + "'");
            }
            out.writeByte(columnCount);
            for (final String cell : row) {
                if (cell == null) {
                    out.writeByte(0);
                } else {
                    final int length = cell.length();
                    if (length > 127) {
                        throw new Error("Serialisering av endring med lengde " + length + " feila, øvre grense for endringslengde er 127 tegn, verdi = '" + cell + "'");
                    }
                    out.writeByte(length);
                    out.writeBytes(cell);
                }
            }
        }
    }

    @Override
    public List<List<String>> read(final ObjectDataInput in) throws IOException {
        final byte[] buffer = new byte[256];
        final short rowCount = in.readShort();
        final Endringer rows = new Endringer(rowCount);
        for (int i = 0; i < rowCount; i++) {
            final byte columnCount = in.readByte();
            final ArrayList<String> row = new ArrayList<>(columnCount);
            for (int j = 0; j < columnCount; j++) {
                final byte length = in.readByte();
                if (length > 0) {
                    in.readFully(buffer, 0, length);
                    row.add(new String(buffer, 0, length, "ASCII"));
                } else {
                    row.add(null);
                }
            }
            rows.add(row);
        }
        return rows;
    }
}