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
        return _bis.readUTF();
	}

	@Override
	public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeBoolean(true);
            return;
        }
        _bos.writeBoolean(false);
        if ((o != null) && (o.getClass().getName().indexOf("CLOB") >= 0) ){
            try {
            	Clob clob = (Clob)o;
    			_bos.writeUTF(clob.getSubString(1, (int) clob.length()));
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        } else {
			_bos.writeUTF((String)o);
	}
	}
}
