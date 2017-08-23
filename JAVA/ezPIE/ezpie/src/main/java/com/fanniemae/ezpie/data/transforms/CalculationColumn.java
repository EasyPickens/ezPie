/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import java.io.File;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-06-20
 * 
 */

public class CalculationColumn extends DataTransform {

	protected String _jsFunctionFile = "";
	protected String _formula = "";
	protected ScriptEngine _engine;
	
	protected int _errorLimit = 1;
	protected int _errors = 0;

	public CalculationColumn(SessionManager session, Element transform) {
		super(session, transform);


		_jsFunctionFile = "";
		String jsFile = getOptionalAttribute("JavaScriptFile", "");
		if (!jsFile.isEmpty()) {
			if (FileUtilities.isInvalidFile(jsFile)) {
				String resourceDir = String.format("[Configuration.ApplicationPath]%s_Resources%s%s", File.separator, File.separator, jsFile);
				resourceDir = _session.resolveTokens(resourceDir);
				if (FileUtilities.isValidFile(resourceDir)) {
					jsFile = resourceDir;
				} else {
					throw new RuntimeException(String.format("JavaScript file %s was not found.", jsFile));
				}
			}
			_jsFunctionFile = FileUtilities.loadFile(jsFile) + "\n";
		}

		_formula = getRequiredAttribute("Formula");
		
		_columnType = getOptionalAttribute("ResultType","String");
		_errorLimit = StringUtilities.toInteger(getOptionalAttribute("ErrorLimit"),1);
		
		ScriptEngineManager manager = new ScriptEngineManager();
		// Check for the newer nashorn engine first, then if not found default to JavaScript.
		_engine = manager.getEngineByName("nashorn");
		if (_engine == null) {
			_engine = manager.getEngineByName("JavaScript");
			if (_engine == null) {
				throw new RuntimeException("Could not find a valid Javascript engine to evaluate expressions.");
			}
		}
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		_session.setDataTokens(DataUtilities.dataRowToTokenHash(_inputSchema, dataRow));
		String resolvedForumla = _jsFunctionFile + _session.resolveTokens(_formula);
		
		dataRow = addDataColumn(dataRow);
		dataRow[_outColumnIndex] = evaluate(resolvedForumla);
		_session.clearDataTokens();
		_rowsProcessed++;
		return dataRow;
	}
	
	private Object evaluate(String expression) {
		try {
			// evaluate the JavaScript expression
			return _engine.eval(expression);
		} catch (ScriptException e) {
			_errors++;
			_session.addLogMessage(String.format("*** Warning #%d ***",_errors),"Evaluation", String.format("Could not evaluate JavaScript formula \"%s\". Reason: %s", expression, e.getMessage()));
			if (_errors >= _errorLimit)
			   throw new RuntimeException(String.format("Calculation formula evaluation error limit of %d reached. The ErrorLimit attribute of the Calculation transform controls this behavior.", _errorLimit), e);
		}
		return null;
	}

}
