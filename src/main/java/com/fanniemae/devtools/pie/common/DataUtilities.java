package com.fanniemae.devtools.pie.common;

import java.sql.Types;

import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-23
 * 
 */
public class DataUtilities {

	public static int dbStringTypeToJavaSqlType(String dataType) {
		switch (dataType.toLowerCase()) {
		case "bigint":
			return Types.BIGINT;
		case "binary":
			return Types.BINARY;
		case "bstr":
			return Types.LONGNVARCHAR;
		case "boolean":
			return Types.BOOLEAN;
		case "char":
			return Types.CHAR;
		case "currency":
			return Types.DECIMAL;
		case "date":
			return Types.DATE;
		case "dbtime":
			return Types.DATE;
		case "dbtimestamp":
			return Types.TIMESTAMP;
		case "decimal":
			return Types.DECIMAL;
		case "double":
			return Types.DOUBLE;
		case "filetime":
			return Types.TIME;
		case "integer":
			return Types.INTEGER;
		case "longvarbinary":
			return Types.LONGVARBINARY;
		case "longvarchar":
			return Types.LONGVARCHAR;
		case "longvarwchar":
			return Types.LONGNVARCHAR;
		case "numeric":
			return Types.DECIMAL;
		case "single":
			return Types.INTEGER;
		case "smallint":
			return Types.INTEGER;
		case "tinyint":
			return Types.INTEGER;
		case "unsignedbigint":
			return Types.BIGINT;
		case "unsignedint":
			return Types.BIGINT;
		case "unsignedsmallint":
			return Types.INTEGER;
		case "unsignedtinyint":
			return Types.INTEGER;
		case "varbinary":
			return Types.VARBINARY;
		case "varchar":
			return Types.VARCHAR;
		case "variant":
			return Types.OTHER;
		case "varnumeric":
			return Types.DECIMAL;
		case "varwchar":
			return Types.NVARCHAR;
		case "wchar":
			return Types.NCHAR;
		case "guid":
			return Types.VARCHAR;
		default:
			return Types.VARCHAR;
		}
	}

	public static DataType DataTypeToEnum(String sTypeName) {
		if (sTypeName == null) {
			return DataType.StringData;
		}
		switch (sTypeName) {
		case "java.math.BigDecimal":
		case "BigDecimalData":
			return DataType.BigDecimalData;
		case "java.lang.Boolean":
		case "BooleanData":
			return DataType.BooleanData;
		case "java.lang.Byte":
		case "ByteData":
			return DataType.ByteData;
		case "java.lang.Character":
		case "CharData":
			return DataType.CharData;
		case "java.util.Date":
		case "DateData":
			return DataType.DateData;
		case "java.lang.Double":
		case "DoubleData":
			return DataType.DoubleData;
		case "java.lang.Float":
		case "FloatData":
			return DataType.FloatData;
		case "java.lang.Integer":
		case "IntegerData":
			return DataType.IntegerData;
		case "java.lang.Long":
		case "LongData":
			return DataType.LongData;
		case "java.lang.Short":
		case "ShortData":
			return DataType.ShortData;
		case "java.lang.String":
		case "StringData":
			return DataType.StringData;
		case "java.sql.Timestamp":
		case "SqlTimestampData":
			return DataType.SqlTimestampData;
		case "java.lang.Object":
		case "ObjectData":
			return DataType.ObjectData;
		default:
			throw new RuntimeException(String.format("Error during DataTypeToEnum conversion. %s type name not supported.", sTypeName));
		}
	}
}
