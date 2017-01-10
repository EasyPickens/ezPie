package com.fanniemae.devtools.pie.common;

import org.junit.Test;

import junit.framework.TestCase;

/**
*
* @author Richard Monson
* @since 2017-01-05
* 
*/

public class ScriptUtilitiesTest extends TestCase {

	@Test
	public void testSimpleTrue() {
		assertTrue("Simple true", ScriptUtilities.evalToBoolean("true"));
	}

	@Test
	public void testSimpleFalse() {
		assertFalse("Simple false", ScriptUtilities.evalToBoolean("false"));
	}

	@Test
	public void testExpression1() {
		runTest("1==1", true);
	}

	@Test
	public void testExpression2() {
		runTest("'dog' != 'cat'", true);
	}

	@Test
	public void testExpression3() {
		runTest("((23==23) && (4==4)) || (1==2)", true);
	}

	@Test
	public void testExpression4() {
		runTest("((23==23) && (4==3)) || (1==2)", false);
	}

	@Test
	public void testExpression5() {
		runTest("(('dog'=='dog') && (3==3)) || ('house'=='boat')", true);
	}

	@Test
	public void testExpression6() {
		runTest("('DOG'.toLowerCase()=='dog')", true);
	}

	@Test
	public void testExpression7() {
		runTest("25+25==50", true);
	}

	@Test
	public void testExpression8() {
		runTest("100/2 == 50", true);
	}

	@Test
	public void testExpression9() {
		runTest("5/2 == 2.5", true);
	}

	@Test
	public void testExpression10() {
		String jscript = "var test = function (a, b) {return a * b};  test(3,3) == 9";
		runTest(jscript, true);
	}

	private static void runTest(String expression, boolean outcome) {
		boolean result = ScriptUtilities.evalToBoolean(expression);
		assertEquals("Expression is " + expression, outcome, result);
	}

}
