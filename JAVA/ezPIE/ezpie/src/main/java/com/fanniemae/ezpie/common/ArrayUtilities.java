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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-07
 * 
 */

public final class ArrayUtilities {

	private ArrayUtilities() {
	}

	public static int indexOf(String[][] items, String target) {
		return indexOf(items, target, 0, true);
	}

	public static int indexOf(String[][] items, String sTarget, int dimension) {
		return indexOf(items, sTarget, dimension, true);
	}

	public static int indexOf(String[][] items, String target, boolean ignoreCase) {
		return indexOf(items, target, 0, ignoreCase);
	}

	public static int indexOf(String[][] items, String target, int dimension, boolean ignoreCase) {
		if (items == null) {
			return -1;
		}
		if ((dimension > items.length) || (dimension < 0)) {
			return -1;
		}

		// int nLength = aItems[nDimension].length;
		int length = items.length;
		if (length == 0) {
			return -1;
		}

		int index = -1;
		for (int i = 0; i < length; i++) {
			if (items[i] == null) {
				continue;
			} else if ((items[i][dimension] == null) && (target != null)) {
				continue;
			} else if ((items[i][dimension] == null) && (target == null)) {
				index = i;
				break;
			} else if (items[i][dimension].equals(target) || (ignoreCase && items[i][dimension].equalsIgnoreCase(target))) {
				index = i;
				break;
			}
		}

		return index;
	}

	public static boolean contains(String[] items, String target) {
		return indexOf(items, target) > -1;
	}

	public static boolean notContains(String[] items, String target) {
		return indexOf(items, target) == -1;
	}

	public static int indexOf(String[] items, String target) {
		if ((items == null) || (target == null) || (items.length == 0)) {
			return -1;
		}

		for (int i = 0; i < items.length; i++) {
			if (target.equals(items[i])) {
				return i;
			}
		}

		return -1;
	}

	public static int indexOf(int[] items, int target) {
		int length = items.length;
		if (length == 0) {
			return -1;
		}

		int index = -1;
		for (int i = 0; i < length; i++) {
			if (items[i] == target) {
				index = i;
				break;
			}
		}

		return index;
	}

	public static Object resizeArray(Object oldArray, int newSize) {
		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class<?> elementType = oldArray.getClass().getComponentType();
		Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0)
			System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
		return newArray;
	}

	public static String toString(String[] lines) {
		ReportBuilder rb = new ReportBuilder();
		rb.appendArray(lines);
		return rb.toString();
	}

	public static String toCommandLine(String[] arguments) {
		if ((arguments == null) || (arguments.length == 0))
			return "";

		StringBuilder sb = new StringBuilder();
		int iLen = arguments.length;
		for (int i = 0; i < iLen; i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(arguments[i]);
		}
		return sb.toString();
	}

	public static String[] returnMatches(String[] items, String match) {
		return filter(items, match, false);
	}

	public static String[] removeMatches(String[] items, String match) {
		return filter(items, match, true);
	}

	protected static String[] filter(String[] items, String regex, boolean remove) {
		regex = "(?i)" + regex.replace(".", "\\.").replace("*", ".*");
		Pattern pattern = Pattern.compile(regex);

		List<String> results = new ArrayList<String>();
		for (int i = 0; i < items.length; i++) {
			boolean match = pattern.matcher(items[i]).matches();
			if (match && !remove) {
				results.add(items[i]);
			} else if (!match && remove) {
				results.add(items[i]);
			}
		}
		return results.toArray(new String[results.size()]);
	}
}
