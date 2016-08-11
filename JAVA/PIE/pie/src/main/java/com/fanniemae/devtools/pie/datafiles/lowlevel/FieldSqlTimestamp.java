package com.fanniemae.devtools.pie.datafiles.lowlevel;

import java.io.IOException;
import java.util.Date;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class FieldSqlTimestamp extends FieldReadWrite {

    public FieldSqlTimestamp(BinaryInputStream streamIn) {
        super(streamIn);
    }

    public FieldSqlTimestamp(BinaryOutputStream streamOut) {
        super(streamOut);
    }

    @Override
    public Object Read() throws IOException {
        long l = _bis.readLong();
        if (l == 0) {
            return null;
        }
        return new Date(l);
    }

    @Override
    public void Write(Object o, Boolean bIsNull) throws IOException {
        if (bIsNull) {
            _bos.writeLong(0);
            return;
        }
        java.sql.Timestamp dtSql = (java.sql.Timestamp) o;
        Date dtValue = new Date(dtSql.getTime());
        _bos.writeLong(dtValue.getTime());
    }
}
