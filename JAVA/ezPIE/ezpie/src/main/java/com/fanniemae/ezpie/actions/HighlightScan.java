/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.Keyboard;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Tara Tritt
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-30
 * 
 */

public class HighlightScan extends Action {

	protected String _destination;
	protected String _dbDeliveryToolPath;
	protected String _hlAgentPath;
	protected String _source;
	protected Process _process = null;
	protected Robot _robot = null;

	// a unique color in the GUI that appears in the middle of the application
	// currently this is an orange gear towards the top of the 'Scan Results' page
	protected Color uniqueColorScanResultsPage = new Color(238, 162, 54);
	// Color of button to discover files
	protected Color buttonColorOnDiscoverPage = new Color(251, 192, 14);

	public HighlightScan(SessionManager session, Element action) {
		super(session, action);
		_destination = requiredAttribute("Destination");
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		// Get a list of Highlight Scan steps
		_session.setDataTokens(dataTokens);
		highlightActions(_action);
		_session.clearDataTokens();
		return null;
	}

	protected void highlightActions(Node node) {
		NodeList nl = XmlUtilities.selectNodes(node, "*");
		int steps = nl.getLength();
		_session.addLogMessage("", "HighlightScan", String.format("%,d steps found", steps));
		for (int i = 0; i < steps; i++) {
			Node nodeStep = nl.item(i);
			String stepAction = nodeStep.getNodeName();
			switch (stepAction) {
			case "FindCastExtractionFiles":
				findCastExtractionFiles(nodeStep);
				break;
			case "RunHighlightAgent":
				runHighlightAgent(nodeStep);
				break;
			}
		}
	}

	protected void findCastExtractionFiles(Node nodeStep) {
		_dbDeliveryToolPath = _session.getAttribute(nodeStep, "DBDeliveryToolPath");
		if (_dbDeliveryToolPath == null) {
			throw new PieException("HLAgentPath not found in definition");
		}
		_session.addLogMessage("", "HighlightScan", "Looking for .castextraction files.");
		checkForCASTDBFiles();
	}

	protected void runHighlightAgent(Node nodeStep) {
		_hlAgentPath = _session.getAttribute(nodeStep, "HLAgentPath");
		if (_hlAgentPath == null) {
			throw new PieException("HLAgentPath not found in definition");
		}
		_session.addLogMessage("", "HighlightScan", "Running Highlight Agent at " + _hlAgentPath);
		try {
			this._robot = new Robot();
		} catch (AWTException e) {
			throw new PieException("Error while trying to start Highlight scan.", e);
		}
		startAgent();
		discoverFiles();
		scanFiles();
		confirmResults();
		saveResults();
		goToFolder();
		this._process.destroy();
	}

	private void checkForCASTDBFiles() {
		File dir = new File(_destination + "\\Analyzed");
		checkForCASTDBFiles(dir);
	}

	private void checkForCASTDBFiles(File dir) {
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.isDirectory()) {
					checkForCASTDBFiles(child);
				} else if (child.isFile()) {
					if ("castextraction".equals(FilenameUtils.getExtension(child.getName()))) {
						_session.addLogMessage("", "HighlightScan", "Found .castextraction files.");
						_session.addLogMessage("", "HighlightScan", "Running CAST DB Delivery Tool at " + _dbDeliveryToolPath + " . Please do not type/click anywhere, until the program is done executing.");
						changeOracleExtractExtension(child);
					}
				}
			}
		}
	}

	private void changeOracleExtractExtension(File castExtractionFile) {
		ProcessBuilder pb = new ProcessBuilder("java", "-jar", _dbDeliveryToolPath);
		Process process = null;
		Robot robot = null;
		try {
			process = pb.start();
			robot = new Robot();
		} catch (IOException | AWTException e) {
			throw new PieException("Could not start Oracle extract.", e);
		}

		sleep(15000);
		// navigating to input text
		for (int i = 0; i < 3; i++) {
			keyPressRelease(KeyEvent.VK_TAB, 200);
			sleep(200);
		}

		// select populated target folder path
		keyPressReleaseControlA(500);

		// entering target folder path
		Keyboard keyboard = new Keyboard(robot);
		keyboard.type(castExtractionFile.getParent() + "\\deliveryResults");

		// navigate to options
		keyPressReleaseShiftTab(200);
		keyPressRelease(KeyEvent.VK_RIGHT, 500);
		for (int i = 0; i < 2; i++) {
			keyPressRelease(KeyEvent.VK_TAB, 200);
			sleep(200);
		}

		// select populated extraction file path
		keyPressReleaseControlA(500);

		// entering target folder path that contains .castextraction file
		keyboard.type(castExtractionFile.getPath());
		sleep(500);
		keyPressRelease(KeyEvent.VK_TAB, 200);
		sleep(500);

		// navigate to menu bar to select Application/Run Application since tabbing to 'Run Application' button
		// and pressing enter does not execute run
		keyPressRelease(KeyEvent.VK_ALT, 500);
		sleep(500);
		for (int i = 0; i < 2; i++) {
			keyPressRelease(KeyEvent.VK_RIGHT, 200);
		}
		keyPressRelease(KeyEvent.VK_ENTER, 200);
		keyPressRelease(KeyEvent.VK_DOWN, 500);
		keyPressRelease(KeyEvent.VK_ENTER, 200);

		sleep(5000);
		process.destroy();
	}

	private void startAgent() {
		_session.addLogMessage("", "HighlightScan", "Starting Highlight Agent. Please do not type/click anywhere, until the program is done executing.");
		sleep(3000);
		try {
			this._process = new ProcessBuilder(_hlAgentPath).start();
		} catch (IOException e) {
			throw new PieException("Error while trying to start Highlight Agent.", e);

		}
	}

	private boolean checkIfColorInRange(Color colorFound, Color toCompare, int range) {
		int red = colorFound.getRed() - toCompare.getRed();
		int green = colorFound.getGreen() - toCompare.getGreen();
		int blue = colorFound.getBlue() - toCompare.getBlue();
		if (Math.abs(red) < range && Math.abs(green) < range && Math.abs(blue) < range)
			return true;
		return false;
	}

	private void discoverFiles() {
		// wait until 'Discover Files' is viewable
		int xx = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2);
		int yy = 0;
		boolean atButtonLocation = false;
		while (!atButtonLocation) {
			yy += 5;
			Color color = this._robot.getPixelColor(xx, yy);
			if (checkIfColorInRange(color, buttonColorOnDiscoverPage, 10))
				atButtonLocation = true;
			if (yy > 1000)
				yy = 0;
		}
		// navigating to input text
		for (int i = 0; i < 3; i++) {
			keyPressRelease(KeyEvent.VK_TAB, 200);
			sleep(200);
		}
		// entering path to analyzed folder
		keyPressReleaseControlA(500);
		Keyboard keyboard = new Keyboard(this._robot);
		keyboard.type(_destination + "\\Analyzed");
		sleep(200);

		// add folder path
		keyPressRelease(KeyEvent.VK_ENTER, 500);
		sleep(500);

		// click on 'Discover Files'
		atButtonLocation = false;
		while (!atButtonLocation) {
			yy += 5;
			Color color = this._robot.getPixelColor(xx, yy);
			if (checkIfColorInRange(color, buttonColorOnDiscoverPage, 10)) {
				atButtonLocation = true;
			}
		}

		moveMouseAndClick(xx, yy);

	}

	private void scanFiles() {
		// rgb(82,85,82) - top dark grey triangle holding gear
		// rgb(239,162,49) - top orange gear

		// wait until 'Select files to scan' page has loaded, by seeing when unique color is viewable
		int xx = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2);
		int yy = 0;

		boolean onScanFilesPage = false;
		while (!onScanFilesPage) {
			yy += 1;
			Color color = this._robot.getPixelColor(xx, yy);
			if (checkIfColorInRange(color, uniqueColorScanResultsPage, 10)) {
				onScanFilesPage = true;
				for (int i = 0; i < 3; i++) {
					moveMouseAndClick(xx, yy);
				}
			}
			if (yy > 300)
				yy = 0;
		}

		// select 'Scan Files'
		keyPressReleaseShiftTab(200);
		keyPressRelease(KeyEvent.VK_ENTER, 200);
		sleep(10000);
	}

	private void confirmResults() {
		// wait until application is done scanning, by seeing when unique color is viewable
		int xx = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2);
		int yy = 0;

		boolean onScanFilesPage = false;
		while (!onScanFilesPage) {
			yy += 1;
			Color color = this._robot.getPixelColor(xx, yy);
			if (checkIfColorInRange(color, uniqueColorScanResultsPage, 10)) {
				onScanFilesPage = true;
				for (int i = 0; i < 3; i++) {
					moveMouseAndClick(xx, yy);
				}
			}
			if (yy > 300)
				yy = 0;
		}

		// select 'Confirm Results'
		keyPressReleaseShiftTab(200);
		keyPressRelease(KeyEvent.VK_ENTER, 200);
		sleep(5000);

	}

	private void saveResults() {
		// select 'Save Results'
		keyPressReleaseShiftTab(200);
		keyPressRelease(KeyEvent.VK_ENTER, 200);
		sleep(2000);

		// navigate to Folder input text
		keyPressRelease(KeyEvent.VK_TAB, 500);
		sleep(500);

		keyPressRelease(KeyEvent.VK_BACK_SPACE, 200);

		for (int i = 0; i < 100; i++) {
			keyPressRelease(KeyEvent.VK_DELETE, 100);
		}
		sleep(100);

		// type folder to put results
		Keyboard keyboard = new Keyboard(this._robot);
		keyboard.type(_destination + "\\Results");
		System.out.println("Saving Results to: " + _destination + "Results");

		// press 'OK'
		for (int i = 0; i < 2; i++) {
			keyPressRelease(KeyEvent.VK_TAB, 200);
			sleep(200);
		}
		keyPressRelease(KeyEvent.VK_ENTER, 200);
		sleep(5000);

	}

	private void goToFolder() {
		keyPressRelease(KeyEvent.VK_TAB, 200);
		keyPressRelease(KeyEvent.VK_ENTER, 200);
	}

	private void moveMouseAndClick(int xx, int yy) {
		this._robot.mouseMove(xx, yy);
		this._robot.mousePress(InputEvent.BUTTON1_MASK);
		this._robot.delay(1000); // Click one second
		this._robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}

	private void keyPressRelease(int keyEvent, int delay) {
		this._robot.keyPress(keyEvent);
		sleep(delay);
		this._robot.keyRelease(keyEvent);
	}

	private void keyPressReleaseShiftTab(int delay) {
		this._robot.keyPress(KeyEvent.VK_SHIFT);
		this._robot.keyPress(KeyEvent.VK_TAB);
		sleep(delay);
		this._robot.keyRelease(KeyEvent.VK_SHIFT);
		this._robot.keyRelease(KeyEvent.VK_TAB);
	}

	private void keyPressReleaseControlA(int delay) {
		_robot.keyPress(KeyEvent.VK_CONTROL);
		_robot.keyPress(KeyEvent.VK_A);
		sleep(delay);
		_robot.keyRelease(KeyEvent.VK_CONTROL);
		_robot.keyRelease(KeyEvent.VK_A);
	}

	private void sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			ExceptionUtilities.goSilent(e);
		}
	}

}
