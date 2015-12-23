package com.fanniemae.automation.common;

import java.sql.Types;

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
}
