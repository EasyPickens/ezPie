package com.fanniemae.automation.actions;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.StringUtilities;

public class ExportDelimited extends Action {

	protected String _OutputFilename;
	protected String _Delimiter = "|";
	protected String _DataSetID;
	
	protected DataStream _DataStream;

	protected String[] _IncludedColumns;
	protected int[] _ColumnIndexes;

	protected Boolean _IncludeColumnNames = true;

	public ExportDelimited(SessionManager session, Element eleAction) {
		super(session, eleAction);

		_OutputFilename = _Session.getAttribute(eleAction, "Filename");
		if (StringUtilities.isNullOrEmpty(_OutputFilename))
			throw new RuntimeException("Missing required output filename.");
		
		_Delimiter = _Session.getAttribute(eleAction, "Delimiter", "|");
		_Session.addLogMessage("", "Delimiter", _Delimiter);
		
		_DataSetID = _Session.getAttribute(eleAction, "DataSetID");
		_DataStream = _Session.getDataStream(_DataSetID);
	}

	@Override
	public String execute() {
		return "";
	}

}
