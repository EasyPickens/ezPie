/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.datafiles.lowlevel;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-28
 * 
 */

public class DataFileEnums {
	public enum DataType {
		BigDecimalData, 
		BooleanData, 
		ByteData, 
		CharData,
		ClobData,
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
