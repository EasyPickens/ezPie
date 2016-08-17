package com.fanniemae.devtools.pie.common;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptUtilities {

	private ScriptUtilities() {}
	
	public static boolean evalToBoolean(String expression) {
		Object x = false;
		try {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("JavaScript"); //nashorn");

			// evaluate JavaScript code
			x = engine.eval(expression);
		} catch (ScriptException e) {
			throw new RuntimeException(String.format("Could not evaluate JavaScript expression \"%s\" to a boolean result. %s", expression, e.getMessage()));
		}
		if ((x == null) || (x.getClass() != Boolean.class)) {
			return false;
		}
		return (boolean) x;

	}

}
