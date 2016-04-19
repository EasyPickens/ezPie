package com.fanniemae.automation.data.transforms;

import java.util.Calendar;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.datafiles.DataReader;
import com.fanniemae.automation.datafiles.DataWriter;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

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
