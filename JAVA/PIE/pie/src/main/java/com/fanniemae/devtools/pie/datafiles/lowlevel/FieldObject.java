package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;

public class FieldObject extends FieldReadWrite {

	public FieldObject(BinaryInputStream streamIn) {
		super(streamIn);
	}

	public FieldObject(BinaryOutputStream streamOut) {
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
        _bos.writeUTF(o.toString());
	}

}
