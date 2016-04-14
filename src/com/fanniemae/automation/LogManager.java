package com.fanniemae.automation;

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

import com.fanniemae.automation.common.FileUtilities;
import com.fanniemae.automation.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-14
 * 
 */
public class LogManager {

	protected String _LogFilename;
	protected String _TemplatePath;
	protected final String _HtmlFooter = "</table><script>$(\".togglelink\").click(function () { $header = $(this); $content = $header.next(); $content.slideToggle(200, function () {$header.text(function () { return $content.is(\":visible\") ? \"Hide Text\" : \"View Text\";});});});</script></body></html>";
	protected final String _BasicLine = "<tr><td>%1$s&nbsp;</td><td>%2$s&nbsp;</td><td>%3$s&nbsp;</td><td>%4$,.4f</td></tr>\n";
	protected final String _LongTextLine = "<tr><td>%1$s&nbsp;</td><td>%2$s&nbsp;</td><td><div class=\"longtexttoggle\"><div class=\"togglelink\"><span>View Text</span></div><div class=\"togglecontent\">%3$s&nbsp;</div></div></td><td>%4$,.4f</td></tr>\n";
	protected final String _ExceptionRow = "<tr class=\"exceptionRow\"><td>%1$s&nbsp;</td><td>%2$s&nbsp;</td><td>%3$s&nbsp;</td><td>%4$,.4f</td></tr>\n";

	protected byte[] _aHtmlFooter;

	protected int _FooterLength;

	protected long _StartTime = System.currentTimeMillis();

	public LogManager(String templatePath, String logFilename) {
		_FooterLength = _HtmlFooter.length();
		_LogFilename = logFilename;
		_TemplatePath = templatePath;
		_aHtmlFooter = _HtmlFooter.getBytes();
		initializeLog();
	}

	public void addFileDetails(String filename, String logGroup) {
		if (!FileUtilities.isValidFile(_LogFilename))
			return;

		File fi = new File(filename);
		long lastModified = fi.lastModified();
		Date dtModified = new Date(lastModified);

		try (RandomAccessFile raf = new RandomAccessFile(_LogFilename, "rw")) {
			raf.seek(raf.length() - _FooterLength);
			raf.write(String.format(_BasicLine, logGroup, "File Name", fi.getName(), elapsedTime()).getBytes());
			raf.write(String.format(_BasicLine, "", "Full Path", filename, elapsedTime()).getBytes());
			raf.write(String.format(_BasicLine, "", "Last Modified Date", dtModified.toString(), elapsedTime()).getBytes());
			raf.write(String.format(_BasicLine, "", "Size", String.format("%,d bytes", fi.length()), elapsedTime()).getBytes());
			raf.write(_aHtmlFooter);
			raf.close();
		} catch (IOException e) {
			throw new RuntimeException("Error trying to add message to debug page.", e);
		}
	}

	public void addMessage(String logGroup, String event, String description) {
		addMessage(logGroup, event, description, "");
	}

	public void addMessagePreserveLayout(String logGroup, String event, String description) {
		updateLog(false, logGroup, event, description, "", true);
	}

	public void addMessage(String logGroup, String event, String description, String cargo) {
		updateLog(false, logGroup, event, description, cargo, false);
	}

	public void addErrorMessage(Exception ex) {
		addErrorMessage(ex, false);
	}

	public void addErrorMessage(Throwable ex, Boolean isInner) {
		String sLogGroup = isInner ? "" : "** ERROR **";
		String sInner = isInner ? "Inner " : "";

		String sMessage = ex.getMessage();
		if (StringUtilities.isNullOrEmpty(sMessage))
			sMessage = "See stack trace for error details.";
		updateLog(true, sLogGroup, sInner + "Message", sMessage);
		if (!ex.getClass().getName().equals("java.lang.RuntimeException"))
			updateLog(true, "", sInner + "Exception Type", ex.getClass().getName().toString());

		StringBuilder sbStack = new StringBuilder();
		boolean bAddLinebreak = false;
		for (StackTraceElement ele : ex.getStackTrace()) {
			if (bAddLinebreak)
				sbStack.append("\n");
			sbStack.append(ele.toString());
			bAddLinebreak = true;
		}
		updateLog(true, "", sInner + "Details", sbStack.toString());
		Throwable inner = ex.getCause();
		if (inner != null)
			addErrorMessage(inner, true);
	}

	protected void initializeLog() {
		// Read JVM runtime settings
		Runtime oRuntime = Runtime.getRuntime();
		// Read the debug template
		String sTemplate = readTemplateFile(_TemplatePath + "Debug.txt");
		_StartTime = System.currentTimeMillis();
		// Create debug file
		try (FileOutputStream fos = new FileOutputStream(_LogFilename, false)) {
			fos.write(sTemplate.getBytes());
			// Add machine information
			fos.write(String.format(_BasicLine, "Environment Information", "Started", new SimpleDateFormat("MMMM d, yyyy HH:mm:ss").format(new Date()), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "User account name", System.getProperty("user.name"), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "Machine Name", InetAddress.getLocalHost().getHostName(), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JAVA Version", System.getProperty("java.version"), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JAVA Architecture", System.getProperty("sun.arch.data.model") + " bit", elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JAVA Home", System.getProperty("java.home"), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JAVA Vendor", System.getProperty("java.vendor"), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JAVA Class Path", System.getProperty("java.class.path"), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JVM Maximum Memory", String.format("%,d Megabytes", oRuntime.maxMemory() / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JVM Total Allocated Memory", String.format("%,d Megabytes reserved", oRuntime.totalMemory() / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JVM Used Memory", String.format("%,d Megabytes", (oRuntime.totalMemory() - oRuntime.freeMemory()) / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "JVM Free Memory", String.format("%,d Megabytes", oRuntime.freeMemory() / 1048576), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "Operating system name", System.getProperty("os.name"), elapsedTime()).getBytes());
			fos.write(String.format(_BasicLine, "", "User working directory", System.getProperty("user.dir"), elapsedTime()).getBytes());
			fos.write(_aHtmlFooter);
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error trying to create log file. %s", _LogFilename));
		}

		// Easy way to list all the available system properties.
		// Properties props = System.getProperties();
		// Enumeration e = props.propertyNames();
		//
		// while (e.hasMoreElements()) {
		// String key = (String) e.nextElement();
		// sw.writeUTF(String.format(_BasicLine,
		// "",key,props.getProperty(key),ElapsedTime()));
		// }
	}

	protected String readTemplateFile(String filename) {
		if (!FileUtilities.isValidFile(filename))
			throw new RuntimeException(String.format("%s template file not found.", filename));

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			StringBuilder sb = new StringBuilder();
			String sLine = br.readLine();
			while (sLine != null) {
				sb.append(sLine);
				// sb.append("\n");
				sLine = br.readLine();
			}
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error while trying to read %s text file.", filename), e);
		}
	}

	protected double elapsedTime() {
		return (System.currentTimeMillis() - _StartTime) / 1000.0;
	}

	protected void updateLog(Boolean isError, String logGroup, String event, String description) {
		updateLog(isError, logGroup, event, description, "", false);
	}

	protected void updateLog(Boolean isError, String logGroup, String event, String description, String cargo, Boolean preserveLayout) {
		// Skip blank description messages
		if (description == null)
			return;
		if (!FileUtilities.isValidFile(_LogFilename))
			return;

		// Encode the description line and preserve any CRLFs.
		if (preserveLayout) {
			description = StringEscapeUtils.escapeHtml3(description).replace(" ", "&nbsp;").replace("\n", "<br />");
		} else {
			description = StringEscapeUtils.escapeHtml3(description).replace("\n", "<br />");
		}
		
		if (cargo.startsWith("file://")) {
			// add html link line to view the file.
			description = String.format("<a href=\"%2$s\">%1$s</a>",description, cargo.substring(7));
		}

		try (RandomAccessFile raf = new RandomAccessFile(_LogFilename, "rw")) {
			raf.seek(raf.length() - _FooterLength);
			if (isError)
				raf.write(String.format(_ExceptionRow, logGroup, event, description, elapsedTime()).getBytes());
			else if ((description != null) && (description.length() > 300)) 
				raf.write(String.format(this._LongTextLine, logGroup, event, description, elapsedTime()).getBytes());
			else
				raf.write(String.format(_BasicLine, logGroup, event, description, elapsedTime()).getBytes());

			// raf.write("\n".getBytes());
			raf.write(_aHtmlFooter);
		} catch (IOException e) {
			throw new RuntimeException("Error trying to add message to debug page.", e);
		}
	}

}
