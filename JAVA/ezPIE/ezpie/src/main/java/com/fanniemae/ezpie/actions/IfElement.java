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

package com.fanniemae.ezpie.actions;

import java.io.File;
import java.util.HashMap;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.ScriptUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-17
 * 
 */

public class IfElement extends Action {

	protected String _expression = "false";

	public IfElement(SessionManager session, Element action) {
		super(session, action, false);

		_expression = "";
		String jsFile = optionalAttribute("JavaScriptFile", null);
		if (jsFile != null) {
			if (FileUtilities.isInvalidFile(jsFile)) {
				String resourceDir = String.format("%s%s_Resources%s%s", _session.getTokenValue("Configuration","ApplicationPath"), File.separator, File.separator, jsFile);
				if (FileUtilities.isValidFile(resourceDir)) {
					jsFile = resourceDir;
				} else {
					throw new RuntimeException(String.format("JavaScript file %s was not found.", jsFile));
				}
			}
			_expression = FileUtilities.loadFile(jsFile) + "\n";
		}

		_expression += requiredAttribute("Expression");
		if (_expression.toLowerCase().endsWith(".js") && FileUtilities.isValidFile(_expression)) {
			_expression = FileUtilities.loadFile(_expression);
		}
	}

	public boolean evalToBoolean() {
		Boolean result = false;
		if (StringUtilities.isBoolean(_expression)) {
			result = StringUtilities.toBoolean(_expression);
		} else {
			result = ScriptUtilities.evalToBoolean(_session.resolveTokens(_expression));
		}
		_session.addLogMessage("", "Result", result.toString());
		return result;
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		// Element is used to control definition branching, no actual action.
		return "";
	}

}
