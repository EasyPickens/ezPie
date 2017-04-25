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

package com.fanniemae.ezpie.common;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-17
 * 
 */

public final class ScriptUtilities {

	private ScriptUtilities() {
	}

	public static boolean evalToBoolean(String expression) {
		Object result = false;
		try {
			ScriptEngineManager manager = new ScriptEngineManager();
			// Check for the newer nashorn engine first, then if not found default to JavaScript.
			ScriptEngine engine = manager.getEngineByName("nashorn");
			if (engine == null) {
				engine = manager.getEngineByName("JavaScript");
				if (engine == null) {
					throw new RuntimeException("Could not find a valid Javascript engine to evaluate expressions.");
				}
			}

			// evaluate the JavaScript expression
			result = engine.eval(expression);
		} catch (ScriptException e) {
			throw new RuntimeException(String.format("Could not evaluate JavaScript expression \"%s\" to a boolean result. %s", expression, e.getMessage()));
		}
		if ((result == null) || (result.getClass() != Boolean.class)) {
			return false;
		}
		return (boolean) result;
	}
}
