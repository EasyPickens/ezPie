package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;
import java.util.UUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldUUID extends FieldReadWrite {

    public FieldUUID(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldUUID(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        String sUUID = _bis.readUTF();
        if (sUUID.equals("")) {
            return null;
        }
        return UUID.fromString(sUUID);
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeUTF("");
            return;
        }
        _bos.writeUTF(((UUID) o).toString()); //Write UUID to string
    }

}
