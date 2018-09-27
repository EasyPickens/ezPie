package com.fanniemae.ezpie.data.connectors;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.JsonFlattener;
import com.fanniemae.ezpie.common.PieException;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-25
 * 
 */

public class JsonConnector extends DataConnector {
	
	protected List<Map<String,Object>> _data = null;
	
	protected Object[] _dataRow;
	
	protected int _numberRows = 0;
	protected int _rowNumber = 0;
	protected int _columnCount = 0;

	public JsonConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
	}

	@Override
	public Boolean open() {
		try {
			String data = _session.requiredAttribute(_dataSource, "Data");
			if (data.startsWith("file://")) {
				String filename = data.substring(7);
				if (FileUtilities.isInvalidFile(filename)) {
					String resourceDir = String.format("%s%s_Resources%s%s",_session.getApplicationPath(),File.separator,File.separator,filename);
					if (FileUtilities.isValidFile(resourceDir)) {
						filename = resourceDir;
					} else {
						throw new PieException(String.format("JSON file %s was not found.", filename));
					}
				}
				data = FileUtilities.loadFile(filename);
			}
			
			JsonFlattener jf = new JsonFlattener(_session);
			_data = jf.getData(data);
			Map<String,String> schema = jf.getSchema();
			
			_numberRows = _data.size();
			
			int colIndex = 0;
			_dataSchema = new String[schema.size()][2];
			for(Map.Entry<String,String> entry : schema.entrySet()) {
				_dataSchema[colIndex][0] = entry.getKey();
				if ((entry.getValue() == null) || (entry.getValue() == JSONObject.NULL)) {
					_dataSchema[colIndex][1] = "java.lang.String";
				} else {
					_dataSchema[colIndex][1] = entry.getValue();
				}
				colIndex++;
			}
			_columnCount = _dataSchema.length;
		} catch (Exception ex) {
			throw new PieException("Error while trying to read in JSON data. "+ex.getMessage(),ex);
		}

		return true;
	}

	@Override
	public Boolean eof() {
		if (_rowNumber < _numberRows) {
			_dataRow = new Object[_columnCount];
			for (int i=0;i<_columnCount;i++) {
				_dataRow[i] = _data.get(_rowNumber).get(_dataSchema[i][0]);
			}
			_rowNumber++;
			return false;
		}
		return true;

	}

	@Override
	public Object[] getDataRow() {
		return _dataRow;
	}

	@Override
	public void close() {
	}

}
