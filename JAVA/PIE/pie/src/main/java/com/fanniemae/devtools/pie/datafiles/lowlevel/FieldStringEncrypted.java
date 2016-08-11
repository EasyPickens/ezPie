package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

import com.fanniemae.devtools.pie.common.CryptoUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldStringEncrypted extends FieldReadWrite {

    public FieldStringEncrypted(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldStringEncrypted(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        if (_bis.readBoolean()) {
            return null;
        }
        return CryptoUtilities.EncryptDecrypt(_bis.readUTF());
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeBoolean(true);
            return;
        }
        _bos.writeBoolean(false);
        _bos.writeUTF(CryptoUtilities.EncryptDecrypt((String) o));
    }
}
