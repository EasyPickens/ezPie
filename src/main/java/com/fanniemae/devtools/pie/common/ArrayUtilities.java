package com.fanniemae.devtools.pie.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-05
 * 
 */
public class ArrayUtilities {
	public static int indexOf(String[][] items, String target) {
		return indexOf(items, target, 0, false);
	}

	public static int indexOf(String[][] items, String sTarget, int dimension) {
		return indexOf(items, sTarget, dimension, false);
	}

	public static int indexOf(String[][] items, String target, boolean ignoreCase) {
		return indexOf(items, target, 0, ignoreCase);
	}

	public static int indexOf(String[][] items, String target, int dimension, boolean ignoreCase) {
		if (items == null) {
			return -1;
		}
		if (dimension > items.length) {
			return -1;
		}

		// int nLength = aItems[nDimension].length;
		int length = items.length;
		if (length == 0) {
			return -1;
		}

		int index = -1;
		for (int i = 0; i < length; i++) {
			if (items[i][dimension].equalsIgnoreCase(target) || (ignoreCase && items[i][dimension].equalsIgnoreCase(target))) {
				index = i;
				break;
			}
		}

		return index;
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
