package com.fanniemae.devtools.pie.actions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.ArrayUtilities;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-16
 * 
 */
public class RunCommand extends Action {
	protected String _workDirectory;
	protected String _commandLine;

	protected String[] _arguments;

	protected Boolean _waitForExit = true;

	protected int _timeout = 0;
	protected int[] _exitCodes = new int[]{0};
	protected String _acceptableErrorOutput;
	protected boolean _ignoreErrorCode = false;

	public RunCommand(SessionManager session, Element action) {
		this(session, action, false);
	}

	public RunCommand(SessionManager session, Element action, boolean bIDRequired) {
		super(session, action, bIDRequired);

		if (!action.getNodeName().equals("RunCommand"))
			return;

		String workDirectory = _session.getAttribute(action, "WorkDirectory");
		String cmdLine = _session.getAttribute(action, "CommandLine");
		String waitForExit = _session.getAttribute(action, "WaitForExit");
		String timeout = _session.getAttribute(action, "Timeout");
		Boolean makeBatchFile = StringUtilities.toBoolean(_session.getAttribute(action, "MakeBatchFile"), false);

		if (StringUtilities.isNullOrEmpty(workDirectory))
			throw new RuntimeException("No WorkDirectory value specified.");
		if (StringUtilities.isNullOrEmpty(cmdLine))
			throw new RuntimeException("No CommandLine value specified.");

		_workDirectory = workDirectory;
		_commandLine = cmdLine;
		_waitForExit = StringUtilities.toBoolean(waitForExit, true);
		_timeout = parseTimeout(timeout);

		_session.addLogMessage("", "Work Directory", _workDirectory);
		_session.addLogMessage("", "Command Line", _commandLine);
		if (StringUtilities.isNotNullOrEmpty(waitForExit))
			_session.addLogMessage("", "Wait For Exit", _waitForExit.toString());
		if (StringUtilities.isNotNullOrEmpty(timeout))
			_session.addLogMessage("", "Timeout Value", String.format("%,d seconds", _timeout));

		if (makeBatchFile) {
			String batchFilename = FileUtilities.writeRandomFile(_session.getStagingPath(), "bat", cmdLine);
			_session.addLogMessage("", "Created Batch File", batchFilename);
			_arguments = new String[] { batchFilename };
		} else {
			_arguments = parseCommandLine(_commandLine);
		}
	}

	@Override
	public String execute() {
		String sConsoleFilename = FileUtilities.getRandomFilename(_session.getLogPath(), "txt");
		Timer commandTimer = null;
		ProcessBuilder pb = new ProcessBuilder(_arguments);
		pb.directory(new File(_workDirectory));
		pb.redirectErrorStream(true);
		try {
			pb.redirectErrorStream(true);
			Process p = pb.start();
			TimerTask killer = new TimeoutRunCommandManager(p);
			if (_timeout > 0) {
				commandTimer = new Timer();
				commandTimer.schedule(killer, _timeout * 1000);
			}

			try (InputStream is = p.getInputStream(); InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr); FileWriter fw = new FileWriter(sConsoleFilename); BufferedWriter bw = new BufferedWriter(fw);) {
				String line = "";
				boolean bAddLineBreak = false;
				int iLines = 0;
				while ((line = br.readLine()) != null) {
					if (bAddLineBreak)
						bw.append(System.lineSeparator());
					if(_acceptableErrorOutput != null && _acceptableErrorOutput.equals(line.trim()))
						_ignoreErrorCode = true;
					bw.append(line);
					bAddLineBreak = true;
					iLines++;
				}
				if (_waitForExit)
					p.waitFor();
				
				bw.flush();
				bw.close();
				_session.addLogMessage("", "Console Output", String.format("View Console Output (%,d lines)", iLines), "file://" + sConsoleFilename);
			} catch (InterruptedException ex) {
				_session.addErrorMessage(ex);
				throw new RuntimeException("Error while running external command.", ex);
			} finally {
				if (commandTimer != null) {
					commandTimer.cancel();
					if (p.exitValue() != 0)
						throw new RuntimeException(String.format("External command timed out. Update timeout limit (currently %d) or disable it.", _timeout));
				} else {
					if (!_ignoreErrorCode && ArrayUtilities.indexOf(_exitCodes, p.exitValue()) == -1)
						throw new RuntimeException(String.format("External command returned an error code of %d.  View console output for error details.", p.exitValue()));
				}
				_session.addLogMessage("", "Exit Code", p.exitValue() + "");
			}
		} catch (IOException ex) {
			_session.addErrorMessage(ex);
			throw new RuntimeException("Error while running external command.", ex);
		}
		_session.addLogMessage("", "Command", "Completed");
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