/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 *
 */

package com.fanniemae.ezpie.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-30
 *
 */

public abstract class CastAction extends RunCommand {
	protected String _castFolder;

	protected Element _connection;

	protected int _jobKey;

	protected DateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public CastAction(SessionManager session, Element action) {
		super(session, action);
		_castFolder = optionalAttribute("CastFolder", _session.resolveTokens("@CAST.ProgramFolder~"));

		if (FileUtilities.isInvalidDirectory(_castFolder)) {
			throw new RuntimeException(String.format("CastFolder %s does not exist", _castFolder));
		}

		initialize();

		if (_session.updateScanManager()) {
			_connection = _session.getConnection("JavaScanManager");
			if (_connection == null) {
				throw new RuntimeException("Missing JavaScanManager connection element.");
			}
			String key = _session.resolveTokens("@Local.JobKey~");
			if (StringUtilities.isNullOrEmpty(key))
				throw new RuntimeException("Missing job primary key required to update ScanManager status.");
			_jobKey = StringUtilities.toInteger(key, -1);
		}
	}

	protected abstract void initialize();

	protected void executeCastAction(String viewLinkLabel, String timeLabel, String logFilename) {
		_session.addLogMessage("", "Command Line", ArrayUtilities.toCommandLine(_arguments));
		makeBatchFile();
		if (StringUtilities.isNotNullOrEmpty(logFilename)) {
			_session.addLogMessage("", "CAST Log File", viewLinkLabel, "file://" + logFilename);
		}
		long start = System.currentTimeMillis();
		super.executeAction();
		// Uncomment sleep and comment out executeAction when doing local testing.
		// Miscellaneous.sleep(10);
		_session.addLogMessage("", "Completed", String.format(timeLabel, DateUtilities.elapsedTime(start)));
	}
}
