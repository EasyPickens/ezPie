package com.fanniemae.automation.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-16
 * 
 */
public class RunCommand extends Action {
	protected String _WorkDirectory;
	protected String _CommandLine;

	protected String[] _Arguments;

	protected Boolean _WaitForExit = true;

	protected int _Timeout = 0;
	protected int _ExitCode = 0;

	public RunCommand(SessionManager session, Element eleAction) {
		super(session, eleAction);

		if (!eleAction.getNodeName().equals("RunCommand"))
			return;

		String sWorkDirectory = _Session.getAttribute(eleAction, "WorkDirectory");
		String sCmdLine = _Session.getAttribute(eleAction, "CommandLine");
		String sWaitForExit = _Session.getAttribute(eleAction, "WaitForExit");
		String sTimeout = _Session.getAttribute(eleAction, "Timeout");

		if (StringUtilities.isNullOrEmpty(sWorkDirectory))
			throw new RuntimeException("No WorkDirectory value specified.");
		if (StringUtilities.isNullOrEmpty(sCmdLine))
			throw new RuntimeException("No CommandLine value specified.");

		_WorkDirectory = sWorkDirectory;
		_CommandLine = sCmdLine;
		_WaitForExit = StringUtilities.toBoolean(sWaitForExit, true);
		_Timeout = parseTimeout(sTimeout);

		_Session.addLogMessage("", "Work Directory", _WorkDirectory);
		_Session.addLogMessage("", "Command Line", _CommandLine);
		if (StringUtilities.isNotNullOrEmpty(sWaitForExit))
			_Session.addLogMessage("", "Wait For Exit", _WaitForExit.toString());
		if (StringUtilities.isNotNullOrEmpty(sTimeout))
			_Session.addLogMessage("", "Timeout Value", String.format("%,d seconds", _Timeout));

		_Arguments = parseCommandLine(_CommandLine);
	}

	@Override
	public String execute() {
		Timer commandTimer = null;
		ProcessBuilder pb = new ProcessBuilder(_Arguments);
		pb.directory(new File(_WorkDirectory));
		pb.redirectErrorStream(true);
		try {
			pb.redirectErrorStream(true);
			Process p = pb.start();
			TimerTask killer = new TimeoutRunCommandManager(p);
			if (_Timeout > 0) {
				commandTimer = new Timer();
				commandTimer.schedule(killer, _Timeout * 1000);
			}
			try (InputStream is = p.getInputStream(); InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr);) {
				StringBuilder sb = new StringBuilder();
				String line = "";
				boolean bAddLineBreak = false;
				while ((line = br.readLine()) != null) {
					if (bAddLineBreak)
						sb.append("\n");
					sb.append(line);
					bAddLineBreak = true;
				}
				if (_WaitForExit)
					p.waitFor();

				_Session.addLogMessagePreserveLayout("", "Console Output", sb.toString());
			} catch (InterruptedException ex) {
				_Session.addErrorMessage(ex);
				throw new RuntimeException("Error while running external command.", ex);
			} finally {
				if (commandTimer != null) {
					commandTimer.cancel();
					if (p.exitValue() != 0)
						throw new RuntimeException(String.format("External command timed out. Update timeout limit (currently %d) or disable it.", _Timeout));
				}
			}
		} catch (IOException ex) {
			_Session.addErrorMessage(ex);
			throw new RuntimeException("Error while running external command.", ex);
		}
		_Session.addLogMessage("", "Operation", "Completed");
		return null;
	}

	protected String[] parseCommandLine(String commandLine) {
		if (commandLine == null)
			return null;

		List<String> aArgs = new ArrayList<String>();
		boolean bInQuotes = false;
		int iLen = commandLine.length();
		int iStart = 0;
		for (int i = 0; i < iLen; i++) {
			char c = commandLine.charAt(i);
			switch (c) {
			case '"':
				if (bInQuotes) {
					if ((i - 1 > 0) && (commandLine.charAt(i - 1) == '\\'))
						continue;
					aArgs.add(commandLine.substring(iStart, i + 1));
					bInQuotes = false;
					iStart = i + 1;
				} else {
					bInQuotes = true;
				}
				break;
			case ' ':
				if (bInQuotes)
					continue;
				if (iStart == i) {
					iStart += 1;
					continue;
				}
				if (iStart >= iLen)
					break;
				aArgs.add(commandLine.substring(iStart, i));
				iStart = i + 1;
				break;
			}
		}
		if (iStart < iLen)
			aArgs.add(commandLine.substring(iStart));

		return aArgs.toArray(new String[0]);
	}

	protected int parseTimeout(String value) {
		if (StringUtilities.isNullOrEmpty(value))
			return -1;

		char cUnits = 's';

		int iStart = 0;
		int iSeconds = 0;
		int iPosition = 0;
		int iCurrentValue = 0;

		value = value.toLowerCase();
		String[] aNumbers = value.split("d|h|m|s");
		for (int i = 0; i < aNumbers.length; i++) {
			if (StringUtilities.isNullOrEmpty(aNumbers[i]))
				continue;

			iPosition = value.indexOf(aNumbers[i], iStart) + aNumbers[i].length();
			iStart = iPosition + 1;
			cUnits = (iPosition < value.length()) ? value.charAt(iPosition) : 's';
			iCurrentValue = StringUtilities.toInteger(aNumbers[i], 0);

			switch (cUnits) {
			case 'd':
				iSeconds += iCurrentValue * 86400;
				break;
			case 'h':
				iSeconds += iCurrentValue * 3600;
				break;
			case 'm':
				iSeconds += iCurrentValue * 60;
				break;
			case 's':
				iSeconds += iCurrentValue;
				break;
			}
		}
		return (iSeconds < 1) ? -1 : iSeconds;
	}
}

class TimeoutRunCommandManager extends TimerTask {
	private Process _p;

	public TimeoutRunCommandManager(Process p) {
		this._p = p;
	}

	@Override
	public void run() {
		_p.destroy();
	}
}