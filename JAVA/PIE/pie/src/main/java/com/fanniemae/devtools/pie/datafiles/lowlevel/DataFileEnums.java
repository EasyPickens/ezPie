package com.fanniemae.devtools.pie.datafiles.lowlevel;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class DataFileEnums {
	public enum DataType {
		BigDecimalData, 
		BooleanData, 
		ByteData, 
		CharData, 
		DateData, 
		DoubleData, 
		FloatData, 
		IntegerData, 
		LongData, 
		ShortData, 
		SqlTimestampData, 
		StringData, 
		UUIDData,
		ObjectData
	}

	public enum ColumnTypes {
		DataValue, // Column with the potential to contain different values in every row.
		GlobalValue // Column that is identical for every row of the data. E.g. Aggregates
	}

	public enum BinaryFileInfo {
		BufferFirstRow, 
		BufferLastRow, 
		DateCreated, 
		DateExpires, 
		DatFilename, 
		Encrypted, 
		FileType, 
		FingerPrint, 
		RowCount, 
		SchemaXML, 
		FullRowCountKnown
	}

//	public enum SqlFieldType {
//		BasicField, 
//		SqlFormula, 
//		GlobalAggregate, 
//		GroupAggregate
//	}

//	public enum AggregateFunction {
//		Average, 
//		Concat, 
//		Count, 
//		CountNotNull, 
//		CountNull, 
//		DistinctCount, 
//		Max, 
//		Median, 
//		Min, 
//		Mode, 
//		Sum, 
//		StdDev, 
//		UpperQuartile, 
//		LowerQuartile
//	}
//
//	// Basic: Average, Count, NullCount, Max, Min, Sum.
//	// Statistical: DistinctCount, Median, Mode, StdDev, UpperQuartile,
//	// LowerQuartile.
//	// FrequencyDistribution = Save copy of column breakdown - expensive.
//	public enum ProfileMetric {
//		Average, 
//		Count, 
//		NullCount, 
//		DistinctCount, 
//		FrequenceDistribution, Max, Median, Min, Mode, Sum, StdDev, UpperQuartile, LowerQuartile
//	}

}
