package com.fanniemae.automation.data.transforms;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.ArrayUtilities;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.ReportBuilder;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.datafiles.DataReader;
import com.fanniemae.automation.datafiles.DataWriter;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-07
 * 
 */
public abstract class DataTransform {
	protected SessionManager _Session;
	protected Element _Transform;

	protected String _ID;
	protected String _TransformName;
	protected String _ExceptionID;
	protected String _ExceptionFilename;
	protected String _DataColumn;
	protected String _ColumnType = "java.lang.String";

	protected int _OutColumnIndex;
	protected int _SourceColumnIndex;
	protected int _RowsProcessed;
	protected int _RowsRemaining;
	protected int _RowsReturned;
	protected int _RowsRemoved;

	protected Boolean _IDRequired = true;
	protected Boolean _NewColumn;
	protected Boolean _AddedNewColumn;
	
	protected ReportBuilder _TransformInfo = new ReportBuilder();

	public DataTransform(SessionManager session, Element operation) {
		this(session, operation, true);
	}

	public DataTransform(SessionManager session, Element transform, boolean idRequired) {
		_Session = session;
		_Transform = transform;
		_IDRequired = idRequired;

		_ID = _Session.getAttribute(transform, "ID");
		_TransformName = transform.getNodeName();
		String sType = transform.getAttribute("Type");
		if (StringUtilities.isNotNullOrEmpty(sType)) {
			_TransformName += "." + sType;
		}

		if (_IDRequired && StringUtilities.isNullOrEmpty(_ID)) {
			throw new RuntimeException(String.format("{0} must have an ID value defined.", _TransformName));
		} 
		if (StringUtilities.isNotNullOrEmpty(_ID)) {
			_TransformInfo.appendFormatLine("ID = %s", _ID);
		}

		_ExceptionID = _Transform.getAttribute("ExceptionDataID");
		if (StringUtilities.isNotNullOrEmpty(_ExceptionID)) {
			_ExceptionFilename = FileUtilities.getDataFilename(_Session.getStagingPath(), XmlUtilities.getOuterXml(_Transform), _ExceptionID);
			_TransformInfo.appendFormatLine("ExceptionID = %s",_ExceptionID);
			_TransformInfo.appendFormatLine("Exception Filename = %s", _ExceptionFilename);
		}
	}

	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		String sTempFilename = FileUtilities.getRandomFilename(_Session.getStagingPath());
		try (DataReader br = new DataReader(inputStream); DataWriter bw = new DataWriter(sTempFilename, memoryLimit)) {
			String[] aColumnNames = br.getColumnNames();
			DataType[] aDataTypes = br.getDataTypes();

			bw.setDataColumns(aColumnNames, aDataTypes);
			while (!br.eof()) {
				Object[] aDataRow = processDataRow(br.getDataRow());
				if (aDataRow != null) {
					bw.writeDataRow(aDataRow);
				}
			}

			bw.close();
			outputStream = bw.getDataStream();
		} catch (Exception ex) {
			throw new RuntimeException(String.format("Error while running %s data stream transformation.", _TransformName), ex);

		}
		return outputStream;
	}

	public abstract boolean isTableLevel();

	public abstract Object[] processDataRow(Object[] dataRow);

	public String[][] UpdateSchema(String[][] aSchema) {
		_OutColumnIndex = ArrayUtilities.indexOf(aSchema, _ID, true);

		if (StringUtilities.isNotNullOrEmpty(_DataColumn)) {
			_SourceColumnIndex = ArrayUtilities.indexOf(aSchema, _DataColumn, true);
		}

		if (_OutColumnIndex != -1) {
			aSchema[_OutColumnIndex][1] = _ColumnType;
			return aSchema;
		} else {
			int nLength = aSchema.length;
			String[][] aNewSchema = new String[nLength + 1][2];
			for (int i = 0; i < nLength; i++) {
				aNewSchema[i][0] = aSchema[i][0];
				aNewSchema[i][1] = aSchema[i][1];
			}
			_NewColumn = true;
			_OutColumnIndex = nLength;
			aNewSchema[nLength][0] = _ID;
			aNewSchema[nLength][1] = _ColumnType;
			return aNewSchema;
		}
	}

	protected Object[] addDataColumn(Object[] dataRow) {
		Object[] aNewDataRow = new Object[dataRow.length + 1];
		System.arraycopy(dataRow, 0, aNewDataRow, 0, dataRow.length);
		return aNewDataRow;
	}

	protected String[] resizeColumnArray(String[] aColumnNames) {
		return resizeColumnArray(aColumnNames, 1);
	}

	protected String[] resizeColumnArray(String[] aColumnNames, int nNewColumnCount) {
		_AddedNewColumn = true;
		String[] aNewColumnArray = new String[aColumnNames.length + nNewColumnCount];

		System.arraycopy(aColumnNames, 0, aNewColumnArray, 0, aColumnNames.length);
		return aNewColumnArray;
	}

	protected DataType[] resizeDataTypeArray(DataType[] aDataTypes) {
		return resizeDataTypeArray(aDataTypes, 1);
	}

	protected DataType[] resizeDataTypeArray(DataType[] aDataTypes, int nNewColumnCount) {
		DataType[] aNewDataTypeArray = new DataType[aDataTypes.length + nNewColumnCount];

		System.arraycopy(aDataTypes, 0, aNewDataTypeArray, 0, aDataTypes.length);
		return aNewDataTypeArray;
	}
	
	public void addTransformLogMessage() {
		_Session.addLogMessage("", _TransformName, _TransformInfo.toString());
	}

	public int getRowsProcessed() {
		return _RowsProcessed;
	}

	public int getRowsRemaining() {
		return _RowsRemaining;
	}

	public int getRowsRemoved() {
		return _RowsRemoved;
	}

	public String getID() {
		return _ID;
	}

	public String getDataColumn() {
		return _DataColumn;
	}

	public boolean isGlobalValue() {
		return false;
	}

	public boolean isColumnProfile() {
		return false;
	}

	public String getGlobalValue() {
		return "";
	}

	public String getGlobalValueType() {
		return "";
	}

	public List<String[]> getColumnProfile() {
		return new ArrayList<>();
	}

}
