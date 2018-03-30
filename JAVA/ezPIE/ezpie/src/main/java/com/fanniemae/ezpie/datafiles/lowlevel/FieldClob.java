package com.fanniemae.ezpie.datafiles.lowlevel;

import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;

public class FieldClob extends FieldReadWrite {

    public FieldClob(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldClob(BinaryOutputStream streamOut) {
        super(streamOut);
    }

	@Override
	public Object Read() throws IOException {
        if (_bis.readBoolean()) {
            return null;
        }
        int length = _bis.readInt();
        byte[] logBytes = new byte[length];
        _bis.read(logBytes);
        return new String(logBytes);
        // return _bis.readUTF();
	}

	@Override
	public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeBoolean(true);
            return;
        }
        _bos.writeBoolean(false);
        String log = "";
        int length = 0;
        byte[] logBytes = null;
        if ((o != null) && (o.getClass().getName().indexOf("CLOB") >= 0) ){
            try {
            	Clob clob = (Clob)o;
            	log = clob.getSubString(1, (int) clob.length());
            	logBytes = log.getBytes();
            	length = logBytes.length;
            	_bos.writeInt(length);
            	_bos.write(logBytes);
    			//_bos.writeUTF(clob.getSubString(1, (int) clob.length()));
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        } else {
			//_bos.writeUTF((String)o);
        	_bos.writeBoolean(true);
	}
	}
}
