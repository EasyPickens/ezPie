package com.fanniemae.ezpie.data.transforms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.transforms.aggregations.Aggregation;
import com.fanniemae.ezpie.data.transforms.aggregations.Avg;
import com.fanniemae.ezpie.data.transforms.aggregations.Count;
import com.fanniemae.ezpie.data.transforms.aggregations.First;
import com.fanniemae.ezpie.data.transforms.aggregations.Last;
import com.fanniemae.ezpie.data.transforms.aggregations.Max;
import com.fanniemae.ezpie.data.transforms.aggregations.Median;
import com.fanniemae.ezpie.data.transforms.aggregations.Min;
import com.fanniemae.ezpie.data.transforms.aggregations.Mode;
import com.fanniemae.ezpie.data.transforms.aggregations.Sum;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-10-20
 * 
 */

public class Group extends DataTransform {

	protected String[] _columnNames;
	protected int[] _columnIndexes;

	protected int _indexColumnCount;
	protected DataType[] _indexDataTypes;

	protected String[] _inputColumnNames;
	protected DataType[] _inputColumnTypes;

	protected Map<String, List<Aggregation>> _aggregates = new HashMap<String, List<Aggregation>>();
	protected boolean _haveAggregate = false;

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
			_inputColumnNames = dr.getColumnNames();
			_inputColumnTypes = dr.getDataTypes();
			_indexColumnCount = drIndex.getColumnNames().length;
			_indexDataTypes = drIndex.getDataTypes();

			setupAggregations();

			String[] outputColumnNames = new String[_inputColumnNames.length + _aggregates.size()];
			DataType[] outputColumnTypes = new DataType[_inputColumnNames.length + _aggregates.size()];
			for (int i = 0; i < _inputColumnNames.length; i++) {
				outputColumnNames[i] = _inputColumnNames[i];
				outputColumnTypes[i] = _inputColumnTypes[i];
			}
			// Add new aggregate columns
			int x = _inputColumnNames.length;
			for (List<Aggregation> agg : _aggregates.values()) {
				outputColumnNames[x] = agg.get(0).getNewColumnName();
				outputColumnTypes[x] = agg.get(0).getNewColumnType();
				x++;
			}

			boolean firstRowOfGroup = true;
			dw.setDataColumns(_inputColumnNames, _inputColumnTypes);
			IndexDataRow previousRow = null;
			while (!drIndex.eof()) {
				// Read in the index row
				Object[] indexRow = drIndex.getDataRow();

				// Build an index point - it is good for quick comparison evaluation.
				IndexDataRow currentRow = new IndexDataRow(0L, _indexColumnCount - 1);
				for (int i = 0; i < indexRow.length - 1; i++) {
					currentRow.setDataPoint(i, indexRow[i], _indexDataTypes[i], true);
				}

				// Read the original data row
				Object[] dataRow = null;
				if (_haveAggregate) {
					dataRow = dr.getDataRowAt((long) indexRow[indexRow.length - 1]);
					// if new group, the clone the aggregates before evaluating them.
					if ((previousRow != null) && (currentRow.compareTo(previousRow) > 0)) {
						for (Map.Entry<String, List<Aggregation>> kvp : _aggregates.entrySet()) {
							Aggregation agg = kvp.getValue().get(0);
							if (agg != null) {
								kvp.getValue().add(agg.clone());
							}
							firstRowOfGroup = true;
						}
					}

					// // Update the aggregates
					// for (Map.Entry<String, List<Aggregation>> kvp : _aggregates.entrySet()) {
					// Aggregation agg = kvp.getValue().get(kvp.getValue().size() - 1);
					// if (agg != null) {
					// agg.evaluate(dataRow[agg.getDataColumnIndex()]);
					// }
					// }

					// Update the aggregates
					for (List<Aggregation> aggList : _aggregates.values()) {
						Aggregation agg = aggList.get(aggList.size() - 1);
						agg.evaluate(dataRow[agg.getDataColumnIndex()]);
						if (firstRowOfGroup) {
							agg.setGroupValues(currentRow.getIndexValues());
							firstRowOfGroup = false;
						}
					}
				}

				if ((previousRow == null) || (currentRow.compareTo(previousRow) > 0)) {
					// dw.writeDataRow(dr.getDataRowAt((long) indexRow[indexRow.length - 1]));
					if (dataRow == null) {
						dataRow = dr.getDataRowAt((long) indexRow[indexRow.length - 1]);
					}
					dw.writeDataRow(dataRow);
					rowCount++;
					previousRow = currentRow.clone();
				}
			}

			Calendar calendarExpires = Calendar.getInstance();
			if (_localCacheEnabled) {
				calendarExpires.add(Calendar.MINUTE, _localCacheMinutes);
			}
			dw.setFullRowCount(rowCount);
			dw.setBufferFirstRow(1L);
			dw.setBufferLastRow(rowCount);
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true);
			dw.close();
			dr.close();
			drIndex.close();
			outputStream = dw.getDataStream();
			outputStream.setCacheFile(_localCacheEnabled);

			ReportBuilder rb = new ReportBuilder();
			for (Map.Entry<String, List<Aggregation>> kvp : _aggregates.entrySet()) {
				List<Aggregation> aggList = kvp.getValue();
				int length = aggList.size();
				for (int i = 0; i < length; i++) {
					Aggregation agg = aggList.get(i);
					if (agg != null) {
						rb.appendFormat("Group #%d %s %s: %s", i, agg.getGroupValuesAsCSV(), agg.getNewColumnName(), agg.getResult().toString());
					}
				}
			}
			_session.addLogMessage("", "Aggregates Calculated", rb.toString());

			_session.addLogMessage("", "Grouping Completed", String.format("%,d rows (%,d bytes in %s, args)", rowCount, outputStream.getSize(), outputStream.isMemory() ? "memorystream" : "filestream"));
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

	protected void setupAggregations() {
		NodeList aggregations = XmlUtilities.selectNodes(_transform, "*");
		int length = aggregations.getLength();
		if (length == 0) {
			return;
		}
		_session.addLogMessage("", "Group", "Setting up group aggregates");
		for (int i = 0; i < length; i++) {
			Aggregation agg = getAggregate((Element) aggregations.item(i));
			if (agg != null) {
				_haveAggregate = true;
				List<Aggregation> aggList = new ArrayList<Aggregation>();
				aggList.add(agg);
				_aggregates.put(agg.getNewColumnName(), aggList);
			}
		}
		_session.addLogMessage("", "Group", String.format("%,d aggregate operations configured", length));
	}

	protected Aggregation getAggregate(Element aggregate) {
		if (aggregate == null) {
			return null;
		}

		String newColumn = _session.requiredAttribute(aggregate, "Name");
		String dataColumn = _session.requiredAttribute(_transform, "DataColumn");

		int columnIndex = ArrayUtilities.indexOf(_inputColumnNames, dataColumn);
		if (columnIndex == -1) {
			throw new PieException(String.format("%s column was not found in the data set.", dataColumn));
		}

		Aggregation agg = null;

		String name = aggregate.getNodeName();
		switch (name) {
		case "Count":
			agg = new Count(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Avg":
			agg = new Avg(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "First":
			agg = new First(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Last":
			agg = new Last(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Median":
			agg = new Median(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Mode":
			agg = new Mode(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Sum":
			agg = new Sum(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Min":
			agg = new Min(_inputColumnTypes[columnIndex], columnIndex);
			break;
		case "Max":
			agg = new Max(_inputColumnTypes[columnIndex], columnIndex);
			break;
		default:
			return null;
		}
		agg.setNewColumnName(newColumn);
		return agg;
	}
}
