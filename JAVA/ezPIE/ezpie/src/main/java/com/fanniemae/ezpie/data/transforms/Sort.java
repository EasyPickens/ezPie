/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import java.util.Calendar;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-21
 * 
*/

public class Sort extends Index {

	public Sort(SessionManager session, Element transform) {
		super(session, transform);
	}
	
	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream indexStream = super.processDataStream(inputStream, memoryLimit);
		return writeSortedFile(inputStream, indexStream, memoryLimit);
	}


	protected DataStream writeSortedFile(DataStream inputStream, DataStream indexStream, int memoryLimit) {
		DataStream outputStream = null;
		String sortedFilename = FileUtilities.getRandomFilename(_session.getStagingPath());
		int rowCount = 0;
		try (DataReader dr = new DataReader(inputStream); DataReader drIndex = new DataReader(indexStream); DataWriter dw = new DataWriter(sortedFilename, memoryLimit)) {
			String[] columnNames = dr.getColumnNames();
			DataType[] columnTypes = dr.getDataTypes();
			dw.setDataColumns(columnNames, columnTypes);
			for (IndexDataRow keys : _indexData) {
				dw.writeDataRow(dr.getDataRowAt(keys.getRowStart()));
				rowCount++;
			}
			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE, 30);
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			dr.close();
			outputStream = dw.getDataStream();
			_indexDataList = null;
			_indexData = null;
			_session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", rowCount, outputStream.getSize(), outputStream.IsMemory() ? "memorystream" : "filestream"));
		} catch (Exception ex) {
			throw new RuntimeException("Error while trying to write final sorted file.", ex);
		}
		return outputStream;
	}
}
