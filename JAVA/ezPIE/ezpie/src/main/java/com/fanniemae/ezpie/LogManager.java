/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;

import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-14
 * 
 */
public class LogManager {
	protected enum LogLevel {
		FULL_LOG, FULL_NO_EXTERNAL_FILES, ERROR_ONLY
	}

	protected enum LogFormat {
		HTML, TEXT
	};

	protected LogLevel _logLevel = LogLevel.FULL_LOG;
	protected LogFormat _logFormat = LogFormat.HTML;

	protected String _logFilename;
	protected String _templatePath;
	protected String _newLine = _htmlNewLine;
	protected String _newLineTab = _htmlNewLineTab;
	protected String _footer = _htmlFooter;
	protected String _basicLine = _htmlBasicLine;
	protected String _longTextLine = _htmlLongTextLine;
	protected String _exceptionRow = _htmlExceptionRow;
	protected static final String _systemNewLine = System.getProperty("line.separator");

	protected static final String _htmlNewLine = "<br />";
	protected static final String _htmlNewLineTab = "<br />";
	protected static final String _htmlFooter = "</table><script>$(\".togglelink\").click(function () { $header = $(this); $content = $header.next(); $content.slideToggle(200, function () {$header.text(function () { return $content.is(\":visible\") ? \"Hide Text\" : \"View Text\";});});});</script></body></html>";
	protected static final String _htmlBasicLine = "<tr><td>%1$s&nbsp;</td><td>%2$s&nbsp;</td><td>%3$s&nbsp;</td><td>%4$s</td></tr>\n";
	protected static final String _htmlLongTextLine = "<tr><td>%1$s&nbsp;</td><td>%2$s&nbsp;</td><td><div class=\"longtexttoggle\"><div class=\"togglelink\"><span>View Text</span></div><div class=\"togglecontent\">%3$s&nbsp;</div></div></td><td>%4$s</td></tr>\n";
	protected static final String _htmlExceptionRow = "<tr class=\"exceptionRow\"><td>%1$s&nbsp;</td><td>%2$s&nbsp;</td><td>%3$s&nbsp;</td><td>%4$s</td></tr>\n";

	protected static final String _textNewLine = System.getProperty("line.separator");
	protected static final String _textNewLineTab = System.getProperty("line.separator") + "\t\t\t\t";
	protected static final String _textFooter = "";
	protected static final String _textBasicLine = "%4$s - %1$s %2$s %3$s" + _textNewLine;
	protected static final String _textLongTextLine = "%4$s - %1$s %2$s %3$s" + _textNewLine;
	protected static final String _textExceptionRow = "%4$s - %1$s %2$s %3$s" + _textNewLine;

	protected byte[] _footerByteArray;

	protected int _footerLength;

	protected long _startTime = System.currentTimeMillis();

	public LogManager(String templatePath, String logFilename, String logFormat, String logLevel) {
		_logFilename = logFilename;
		_templatePath = templatePath;

		if (logLevel != null) {
			switch (logLevel.toLowerCase()) {
			case "":
			case "full":
				_logLevel = LogLevel.FULL_LOG;
				break;
			case "full_log_only":
				_logLevel = LogLevel.FULL_NO_EXTERNAL_FILES;
				break;
			case "error_only":
				_logLevel = LogLevel.ERROR_ONLY;
				break;
			default:
				throw new RuntimeException(String.format("%s is not a recognized log level. Only Full, Full_Log_Only, and Error_Only are currently supported.", logLevel));
			}
		}

		if ((logFormat != null) && ("Text".equalsIgnoreCase(logFormat))) {
			_logFormat = LogFormat.TEXT;
			_footerLength = 0;
			_newLine = _textNewLine;
			_newLineTab = _textNewLineTab;
			_footer = _textFooter;
			_footerByteArray = new byte[] {};
			_basicLine = _textBasicLine;
			_longTextLine = _textLongTextLine;
			_exceptionRow = _textExceptionRow;
		} else {
			_footerLength = _htmlFooter.length();
			_footerByteArray = _htmlFooter.getBytes();
		}
		if (_logLevel != LogLevel.ERROR_ONLY) {
			initializeLog();
		}
	}

	public void addFileDetails(String filename, String logGroup) {
		if (_logLevel == LogLevel.ERROR_ONLY) {
			return;
		}
		
		if (!FileUtilities.isValidFile(_logFilename))
			return;

		File fi = new File(filename);
		long lastModified = fi.lastModified();
		Date dtModified = new Date(lastModified);

		try (RandomAccessFile raf = new RandomAccessFile(_logFilename, "rw")) {
			raf.seek(raf.length() - _footerLength);
			raf.write(String.format(_basicLine, logGroup, "File Name", fi.getName(), elapsedTime()).getBytes());
			// Turning off the full path on log, could be security concern.
			// raf.write(String.format(_basicLine, "", "Full Path", filename, elapsedTime()).getBytes());
			raf.write(String.format(_basicLine, groupString(""), "Last Modified Date", dtModified.toString(), elapsedTime()).getBytes());
			raf.write(String.format(_basicLine, groupString(""), "Size", String.format("%,d bytes", fi.length()), elapsedTime()).getBytes());
			raf.write(_footerByteArray);
			raf.close();
		} catch (IOException e) {
			throw new RuntimeException("Error trying to add message to debug page.", e);
		}
	}

	public void addMessage(String logGroup, String event, String description) {
		addMessage(logGroup, event, description, "");
	}

	public void addMessagePreserveLayout(String logGroup, String event, String description) {
		updateLog(false, logGroup, event, description, "", true, false);
	}

	public void addMessage(String logGroup, String event, String description, String cargo) {
		updateLog(false, logGroup, event, description, cargo, false, false);
	}

	public void addHtmlMessage(String logGroup, String event, String description, String cargo) {
		updateLog(false, logGroup, event, description, cargo, false, true);
	}

	public void addErrorMessage(Exception ex) {
		addErrorMessage(ex, false);
	}

	public void addErrorMessage(Throwable ex, Boolean isInner) {
		String logGroup = isInner ? "" : "** ERROR **";
		String inner = isInner ? "Inner " : "";

		String message = ex.getMessage();
		if (StringUtilities.isNullOrEmpty(message))
			message = "See stack trace for error details.";
		updateLog(true, logGroup, inner + "Message", message);
		if (!ex.getClass().getName().equals("java.lang.RuntimeException"))
			updateLog(true, "", inner + "Exception Type", ex.getClass().getName().toString());

		StringBuilder sbStack = new StringBuilder();
		boolean addLinebreak = false;
		for (StackTraceElement ele : ex.getStackTrace()) {
			if (addLinebreak)
				sbStack.append(getNewLineTab());
			sbStack.append(ele.toString());
			addLinebreak = true;
		}
		updateLog(true, "", inner + "Details", sbStack.toString());
		Throwable innerException = ex.getCause();
		if (innerException != null)
			addErrorMessage(innerException, true);
	}

	public String getNewLineTab() {
		if (_logFormat == LogFormat.TEXT)
			return _newLineTab;

		return _systemNewLine;
	}

	public boolean logExternalFiles() {
		return _logLevel == LogLevel.FULL_LOG;
	}
	
	protected void initializeLog() {
		// Read JVM runtime settings
		Runtime runtime = Runtime.getRuntime();
		// Read the debug template
		String template = (_logFormat == LogFormat.HTML) ? readTemplateFile(_templatePath + "Debug.txt") : "";
		_startTime = System.currentTimeMillis();
		// Create debug file
		try (FileOutputStream fos = new FileOutputStream(_logFilename, false)) {
			fos.write(template.getBytes());
			// Add machine information
			fos.write(String.format(_basicLine, "Environment Information", "Started", new SimpleDateFormat("MMMM d, yyyy HH:mm:ss").format(new Date()), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "User account name", System.getProperty("user.name"), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "Machine Name", InetAddress.getLocalHost().getHostName(), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JAVA Version", System.getProperty("java.version"), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JAVA Architecture", System.getProperty("sun.arch.data.model") + " bit", elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JAVA Home", System.getProperty("java.home"), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JAVA Vendor", System.getProperty("java.vendor"), elapsedTime()).getBytes());
			fos.write(String.format(_longTextLine, groupString(""), "JAVA Class Path", System.getProperty("java.class.path").replace(";", ";" + _newLineTab), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JVM Maximum Memory", String.format("%,d Megabytes", runtime.maxMemory() / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JVM Total Allocated Memory", String.format("%,d Megabytes reserved", runtime.totalMemory() / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JVM Used Memory", String.format("%,d Megabytes", (runtime.totalMemory() - runtime.freeMemory()) / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "JVM Free Memory", String.format("%,d Megabytes", runtime.freeMemory() / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "Operating system name", System.getProperty("os.name"), elapsedTime()).getBytes());
			fos.write(String.format(_basicLine, groupString(""), "User working directory", System.getProperty("user.dir"), elapsedTime()).getBytes());
			fos.write(_footerByteArray);
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error trying to create log file. %s", _logFilename));
		}
	}

	protected String readTemplateFile(String filename) {
		if (!FileUtilities.isValidFile(filename))
			throw new RuntimeException(String.format("%s template file not found.", filename));

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				// sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error while trying to read %s text file.", filename), e);
		}
	}

	// protected double elapsedTime() {
	// return (System.currentTimeMillis() - _startTime) / 1000.0;
	// }

	protected String elapsedTime() {
		if (_logFormat == LogFormat.TEXT) {
			return DateUtilities.getCurrentDateTime();
		} else {
			return DateUtilities.elapsedTimeShort(_startTime);
		}
	}

	protected void updateLog(Boolean isError, String logGroup, String event, String description) {
		updateLog(isError, logGroup, event, description, "", false, false);
	}

	protected void updateLog(Boolean isError, String logGroup, String event, String description, String cargo, Boolean preserveLayout, Boolean isHTML) {
		if (!isError && (_logLevel == LogLevel.ERROR_ONLY)) {
			return;
		}
		
		// Skip blank description messages
		if (description == null)
			return;
		if (!FileUtilities.isValidFile(_logFilename))
			initializeLog();

		// Encode the description line and preserve any CRLFs.
		if (isHTML) {
			// do nothing
		} else if (_logFormat == LogFormat.TEXT) {
			// do nothing
		} else if (preserveLayout) {
			description = StringEscapeUtils.escapeHtml3(description).replace(" ", "&nbsp;").replace("\n", _newLine);
		} else {
			description = StringEscapeUtils.escapeHtml3(description).replace("\n", _newLine);
		}

		if ((_logFormat == LogFormat.HTML) && cargo.startsWith("file://")) {
			// add html link line to view the file.
			description = String.format("<a href=\"%2$s\">%1$s</a>", description, cargo.substring(7));
		} else if (cargo.startsWith("file://")) {
			description = String.format("%1$s %2$s", description, cargo.substring(7));
		}

		try (RandomAccessFile raf = new RandomAccessFile(_logFilename, "rw")) {
			raf.seek(raf.length() - _footerLength);
			if (isError)
				raf.write(String.format(_exceptionRow, groupString(logGroup), event, description, elapsedTime()).getBytes());
			else if ((description != null) && (description.length() > 300))
				raf.write(String.format(_longTextLine, groupString(logGroup), event, description, elapsedTime()).getBytes());
			else
				raf.write(String.format(_basicLine, groupString(logGroup), event, description, elapsedTime()).getBytes());

			// raf.write("\n".getBytes());
			raf.write(_footerByteArray);
		} catch (IOException e) {
			throw new RuntimeException("Error trying to add message to debug page.", e);
		}
	}

	protected String groupString(String group) {
		if ((_logFormat == LogFormat.TEXT) && (group == "")) {
			return "\t";
		}

		return group;
	}

}
