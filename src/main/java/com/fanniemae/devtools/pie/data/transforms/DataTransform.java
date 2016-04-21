package com.fanniemae.devtools.pie.data.transforms;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.DataStream;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.ReportBuilder;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;
import com.fanniemae.devtools.pie.datafiles.DataReader;
import com.fanniemae.devtools.pie.datafiles.DataWriter;
import com.fanniemae.devtools.pie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-07
 * 
 */
public abstract class DataTransform {
	protected SessionManager _session;
	protected Element _transform;

	protected String _id;
	protected String _transformName;
	protected String _exceptionID;
	protected String _exceptionFilename;
	protected String _dataColumn;
	protected String _columnType = "java.lang.String";

	protected int _outColumnIndex;
	protected int _sourceColumnIndex;
	protected int _rowsProcessed;
	protected int _rowsRemaining;
	protected int _rowsReturned;
	protected int _rowsRemoved;

	protected Boolean _idRequired = true;
	protected Boolean _newColumn;
	protected Boolean _addedNewColumn;
	
	protected ReportBuilder _transformInfo = new ReportBuilder();

	public DataTransform(SessionManager session, String transformName) {
		_session = session;
		_transformName = transformName;
	}
	
	public DataTransform(SessionManager session, Element transform) {
		this(session, transform, true);
	}

	public DataTransform(SessionManager session, Element transform, boolean idRequired) {
		_session = session;
		_transform = transform;
		_idRequired = idRequired;

		_id = _session.getAttribute(transform, "ID");
		_transformName = transform.getNodeName();
		String sType = transform.getAttribute("Type");
		if (StringUtilities.isNotNullOrEmpty(sType)) {
			_transformName += "." + sType;
		}

		if (_idRequired && StringUtilities.isNullOrEmpty(_id)) {
			throw new RuntimeException(String.format("{0} must have an ID value defined.", _transformName));
		} 
		if (StringUtilities.isNotNullOrEmpty(_id)) {
			_transformInfo.appendFormatLine("ID = %s", _id);
		}

		_exceptionID = _transform.getAttribute("ExceptionDataID");
		if (StringUtilities.isNotNullOrEmpty(_exceptionID)) {
			_exceptionFilename = FileUtilities.getDataFilename(_session.getStagingPath(), XmlUtilities.getOuterXml(_transform), _exceptionID);
			_transformInfo.appendFormatLine("ExceptionID = %s",_exceptionID);
			_transformInfo.appendFormatLine("Exception Filename = %s", _exceptionFilename);
		}
	}
	
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		String sTempFilename = FileUtilities.getRandomFilename(_session.getStagingPath());
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
			throw new RuntimeException(String.format("Error while running %s data stream transformation.", _transformName), ex);

		}
		return outputStream;
	}

	public abstract boolean isTableLevel();

	public abstract Object[] processDataRow(Object[] dataRow);

	public String[][] UpdateSchema(String[][] aSchema) {
		_outColumnIndex = ArrayUtilities.indexOf(aSchema, _id, true);

		if (StringUtilities.isNotNullOrEmpty(_dataColumn)) {
			_sourceColumnIndex = ArrayUtilities.indexOf(aSchema, _dataColumn, true);
		}

		if (_outColumnIndex != -1) {
			aSchema[_outColumnIndex][1] = _columnType;
			return aSchema;
		} else {
			int nLength = aSchema.length;
			String[][] aNewSchema = new String[nLength + 1][2];
			for (int i = 0; i < nLength; i++) {
				aNewSchema[i][0] = aSchema[i][0];
				aNewSchema[i][1] = aSchema[i][1];
			}
			_newColumn = true;
			_outColumnIndex = nLength;
			aNewSchema[nLength][0] = _id;
			aNewSchema[nLength][1] = _columnType;
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
		_addedNewColumn = true;
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
		_session.addLogMessage("", _transformName, _transformInfo.toString());
	}

	public int getRowsProcessed() {
		return _rowsProcessed;
	}

	public int getRowsRemaining() {
		return _rowsRemaining;
	}

	public int getRowsRemoved() {
		return _rowsRemoved;
	}

	public String getID() {
		return _id;
	}

	public String getDataColumn() {
		return _dataColumn;
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
