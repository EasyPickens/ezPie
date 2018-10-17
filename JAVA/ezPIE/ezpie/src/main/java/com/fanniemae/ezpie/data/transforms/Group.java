package com.fanniemae.ezpie.data.transforms;

import java.util.Calendar;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

public class Group extends DataTransform {

	protected String[] _columnNames;
	protected int[] _columnIndexes;

	protected int _indexColumnCount;
	protected DataType[] _indexDataTypes;

	public Group(SessionManager session, Element transform) {
		super(session, transform, false);
		_isolate = true;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new PieException(String.format("%s requires access to the entire data set.  It cannot be processed in the same group as other data transforms.", _transform.getNodeName()));
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		String groupColumnNames = _session.requiredAttribute(_transform, "ColumnNames");
		_columnNames = groupColumnNames.split(",");
		// trim leading and trailing spaces from each entry.
		for (int i = 0; i < _columnNames.length; i++) {
			_columnNames[i] = _columnNames[i].trim();
		}

		// Index the dataStream on the group columns
		_session.addLogMessage("", "Index Data", "Indexing the data.");
		DataTransform indexData = TransformFactory.getIndexTransform(_session, _columnNames);
		DataStream indexStream = indexData.processDataStream(inputStream, memoryLimit);

		_session.addLogMessage("", "Group Data", "Grouping the data.");
		DataStream outputStream = null;
		String outputFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "dat");

		int rowCount = 0;
		//@formatter:off
		try (DataReader dr = new DataReader(inputStream);
			 DataReader drIndex = new DataReader(indexStream);
			 DataWriter dw = new DataWriter(outputFilename, memoryLimit)) {
		     //@formatter:on
			String[] columnNames = dr.getColumnNames();
			DataType[] columnTypes = dr.getDataTypes();
			_indexColumnCount = drIndex.getColumnNames().length;
			_indexDataTypes = drIndex.getDataTypes();
			dw.setDataColumns(columnNames, columnTypes);
			IndexDataRow previousRow = null;
			while (!drIndex.eof()) {
				Object[] indexRow = drIndex.getDataRow();
				IndexDataRow currentRow = new IndexDataRow(0L, _indexColumnCount - 1);

				for (int i = 0; i < indexRow.length - 1; i++) {
					currentRow.setDataPoint(i, indexRow[i], _indexDataTypes[i], true);
				}

				if ((previousRow == null) || (currentRow.compareTo(currentRow) > 0)) {
					dw.writeDataRow(dr.getDataRowAt((long) indexRow[indexRow.length - 1]));
					rowCount++;
					previousRow = currentRow.clone();
				}
			}

			Calendar calendarExpires = Calendar.getInstance();
			calendarExpires.add(Calendar.MINUTE, _session.getCacheMinutes());
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1L);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			dr.close();
			drIndex.close();
			outputStream = dw.getDataStream();
			_session.addLogMessage("", "Grouping Completed", String.format("%,d rows (%,d bytes in %s, args)", rowCount, outputStream.getSize(), outputStream.IsMemory() ? "memorystream" : "filestream"));
		} catch (Exception e) {
			throw new PieException("Error while trying to write final grouped file. " + e.getMessage(), e);
		} finally {
			try {
				indexStream.delete();
			} catch (Exception ex) {
				ExceptionUtilities.goSilent(ex);
			}

		}
		
		return outputStream;
	}

}
