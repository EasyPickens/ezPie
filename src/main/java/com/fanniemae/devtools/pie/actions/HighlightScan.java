package com.fanniemae.devtools.pie.actions;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.Keyboard;
import com.fanniemae.devtools.pie.common.XmlUtilities;

public class HighlightScan extends Action {
	
	protected String _destination;
	protected String _oracleDeliveryToolPath;
	protected String _hlAgentPath;
	protected String _source;
	protected Process process = null;
	protected Robot robot = null;

	//a unique color in the GUI that appears in the middle of the application
	//currently this is an orange gear towards the top of the 'Scan Results' page
	protected Color uniqueColorScanResultsPage = new Color(238, 162, 54);
	//Color of button to discover files 
	protected Color buttonColorOnDiscoverPage = new Color(251, 192, 14);


	public HighlightScan(SessionManager session, Element action) {
		super(session, action);
		_destination = _session.getAttribute(action, "DestinationFolder");
	}

	@Override
	public String execute() {
		// Get a list of Highlight Scan steps
		highlightActions(_action);
		return null;
	}
	
	protected void highlightActions(Node node){
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
	
	protected void findCastExtractionFiles(Node nodeStep){
		_oracleDeliveryToolPath = _session.getAttribute(nodeStep, "OracleDeliveryToolPath");
		checkForCASTDBFiles();
	}
	
	protected void runHighlightAgent(Node nodeStep){
		_hlAgentPath = _session.getAttribute(nodeStep, "HLAgentPath");
		try {
			this.robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		createResultsFolder();
		startAgent();
		discoverFiles();
		scanFiles();
		confirmResults();
		saveResults();
		goToFolder();
		this.process.destroy();
	}
	
    private void changeOracleExtractExtension(File castExtractionFile){
    	ProcessBuilder pb = new ProcessBuilder("java", "-jar", _oracleDeliveryToolPath);
    	Process process = null;
    	try {
			process = pb.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	Robot robot = null;
		try {
			robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        sleep(15000);
        //navigating to input text
        for(int i=0;i<3;i++){
            robot.keyPress(KeyEvent.VK_TAB);
            sleep(200);
            robot.keyRelease(KeyEvent.VK_TAB);
            sleep(200);
        }
        
        //select populated target folder path
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        sleep(500);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_A);
        
        //entering target folder path
        Keyboard keyboard = new Keyboard(robot);
        keyboard.type(castExtractionFile.getParent() + "\\deliveryResults");
        
        //navigate to options
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_TAB);
        sleep(200);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.keyPress(KeyEvent.VK_RIGHT);
        sleep(200);
        robot.keyRelease(KeyEvent.VK_RIGHT);
        for(int i=0;i<2;i++){
            robot.keyPress(KeyEvent.VK_TAB);
            sleep(200);
            robot.keyRelease(KeyEvent.VK_TAB);
            sleep(200);
        }
        
        //select populated extraction file path
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        sleep(500);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_A);
        
        //entering target folder path that contains .castextraction file
        keyboard.type(castExtractionFile.getPath());
        sleep(500);
        robot.keyPress(KeyEvent.VK_TAB);
        sleep(200);
        robot.keyRelease(KeyEvent.VK_TAB);
        sleep(500);

        //navigate to menu bar to select Application/Run Application since tabbing to 'Run Application' button
        //and pressing enter does not execute run
        robot.keyPress(KeyEvent.VK_ALT);
        sleep(500);
        robot.keyRelease(KeyEvent.VK_ALT);
        sleep(500);
        for(int i=0;i<2;i++){
            robot.keyPress(KeyEvent.VK_RIGHT);
            sleep(200);
            robot.keyRelease(KeyEvent.VK_RIGHT);
        }
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        sleep(5000);
        process.destroy();
    }
    
	private void checkForCASTDBFiles(){
		File dir = new File(_destination+"\\Analyzed");
		checkForCASTDBFiles(dir);	
	}
	
	private void checkForCASTDBFiles(File dir){
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
		    for (File child : directoryListing) {
				if (child.isDirectory()) {
					checkForCASTDBFiles(child);
	            } else if(child.isFile()){
					if(FilenameUtils.getExtension(child.getName()).equals("castextraction")){
						System.out.println("\n\n-------------------------------");
			    		System.out.println("Found extracted oracle data.");
			    		System.out.println("Running Oracle DB Delivery Tool. Please do not type/click anywhere, until the program is done executing.");
			    		changeOracleExtractExtension(child);
			    	}
	            }
		    }
		}
	}
	
	private void createResultsFolder(){	
		File dir = new File(_destination+"\\Results");
		if (!dir.exists()) {
			System.out.println("\n\n-------------------------------");
			System.out.println("Creating Results folder: "+_destination+"Results");
        	dir.mkdir();
        }
	}
	
	private void startAgent(){
    	System.out.println("\n\n-------------------------------");
		System.out.println("Starting Highlight Agent. Please do not type/click anywhere, until the program is done executing.");
		sleep(3000);
		try {
			this.process = new ProcessBuilder(_hlAgentPath).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean checkIfColorInRange(Color colorFound, Color toCompare, int range){
		int red = colorFound.getRed() - toCompare.getRed();
		int green = colorFound.getGreen() - toCompare.getGreen();
		int blue = colorFound.getBlue() - toCompare.getBlue();
		if(Math.abs(red) < range && Math.abs(green) < range && Math.abs(blue) < range)
			return true;
		return false;
	}
	
	private void discoverFiles(){	  
		//wait until 'Discover Files' is viewable
        int xx = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2);
        int yy = 0;
        boolean atButtonLocation = false;
        while(!atButtonLocation){
        	yy += 5;
        	Color color = this.robot.getPixelColor(xx, yy);
        	if(checkIfColorInRange(color, buttonColorOnDiscoverPage, 10))
        		atButtonLocation = true;
        	if(yy > 1000)
        		yy = 0;
        }
        //navigating to input text
        for(int i=0;i<3;i++){
        	this.robot.keyPress(KeyEvent.VK_TAB);
            sleep(200);
            this.robot.keyRelease(KeyEvent.VK_TAB);
            sleep(200);
        }
        //entering path to analyzed folder
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        sleep(500);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_A);
        Keyboard keyboard = new Keyboard(this.robot);
        keyboard.type(_destination+"\\Analyzed");
        sleep(200);
        
        //add folder path
        this.robot.keyPress(KeyEvent.VK_ENTER);
        sleep(500);
        this.robot.keyRelease(KeyEvent.VK_ENTER);
        sleep(500);
        
        //click on 'Discover Files'
        atButtonLocation = false;
        while(!atButtonLocation){
        	yy += 5;
        	Color color = this.robot.getPixelColor(xx, yy);
        	if(checkIfColorInRange(color, buttonColorOnDiscoverPage, 10))
        		atButtonLocation = true;
        }
     
        this.robot.mouseMove(xx, yy);
        this.robot.mousePress(InputEvent.BUTTON1_MASK);
        this.robot.delay(1000); // Click one second
        this.robot.mouseRelease(InputEvent.BUTTON1_MASK);
        this.robot.mousePress(InputEvent.BUTTON1_MASK);
        this.robot.delay(1000); // Click one second
        this.robot.mouseRelease(InputEvent.BUTTON1_MASK);
        
	}
	
	
	private void scanFiles(){
		//rgb(82,85,82) - top dark grey triangle holding gear
		//rgb(239,162,49) - top orange gear
        
		//wait until 'Select files to scan' page has loaded, by seeing when unique color is viewable
        int xx = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2);
        int yy = 0;
        
        boolean onScanFilesPage = false;
        while(!onScanFilesPage){
        	yy += 1;
        	Color color = this.robot.getPixelColor(xx, yy);
        	if(checkIfColorInRange(color, uniqueColorScanResultsPage, 10))
        		onScanFilesPage = true;
        	if(yy > 300)
        		yy = 0;
        }
        
        //select 'Scan Files'
		this.robot.keyPress(KeyEvent.VK_SHIFT);
		this.robot.keyPress(KeyEvent.VK_TAB);
        sleep(200);
        this.robot.keyRelease(KeyEvent.VK_SHIFT);
        this.robot.keyRelease(KeyEvent.VK_TAB);
        this.robot.keyPress(KeyEvent.VK_ENTER);
        sleep(200);
        this.robot.keyRelease(KeyEvent.VK_ENTER);
        sleep(10000);
	}
	
	private void confirmResults(){
		//wait until application is done scanning, by seeing when unique color is viewable
		int xx = (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2);
        int yy = 0;
        
        boolean onScanFilesPage = false;
        while(!onScanFilesPage){
        	yy += 1;
        	Color color = this.robot.getPixelColor(xx, yy);
        	if(color.equals(uniqueColorScanResultsPage)){
        		onScanFilesPage = true;
        		for(int i=0;i<3;i++){
	        		this.robot.mouseMove(xx, yy);
	    	        this.robot.mousePress(InputEvent.BUTTON1_MASK);
	    	        this.robot.delay(1000); // Click one second
	    	        this.robot.mouseRelease(InputEvent.BUTTON1_MASK);
        		}
        	}
        	if(yy > 300)
        		yy = 0;
        }
        
      //select 'Confirm Results'
	  this.robot.keyPress(KeyEvent.VK_SHIFT);
	  this.robot.keyPress(KeyEvent.VK_TAB);
      sleep(200);
      this.robot.keyRelease(KeyEvent.VK_SHIFT);
      this.robot.keyRelease(KeyEvent.VK_TAB);
      this.robot.keyPress(KeyEvent.VK_ENTER);
      sleep(200);
      this.robot.keyRelease(KeyEvent.VK_ENTER);
      sleep(5000);
        
	}
	
	private void saveResults(){
      //select 'Save Results'
	  this.robot.keyPress(KeyEvent.VK_SHIFT);
	  this.robot.keyPress(KeyEvent.VK_TAB);
      sleep(200);
      this.robot.keyRelease(KeyEvent.VK_SHIFT);
      this.robot.keyRelease(KeyEvent.VK_TAB);
      this.robot.keyPress(KeyEvent.VK_ENTER);
      sleep(200);
      this.robot.keyRelease(KeyEvent.VK_ENTER);
      sleep(2000);
      
      //navigate to Folder input text 
	  this.robot.keyPress(KeyEvent.VK_TAB);
      sleep(500);
      this.robot.keyRelease(KeyEvent.VK_TAB);
      sleep(500);

      robot.keyPress(KeyEvent.VK_BACK_SPACE);
      sleep(100);
      robot.keyRelease(KeyEvent.VK_BACK_SPACE); 
      
      for(int i=0;i<50;i++){
          robot.keyPress(KeyEvent.VK_DELETE);
          sleep(100);
          robot.keyRelease(KeyEvent.VK_DELETE);      
      }
      sleep(100);
      
      //type folder to put results
      Keyboard keyboard = new Keyboard(this.robot);
      keyboard.type(_destination+"\\Results");
      System.out.println("Saving Results to: "+ _destination +"Results");
      
      //press 'OK'
      for(int i=0;i<2;i++){
          robot.keyPress(KeyEvent.VK_TAB);
          sleep(200);
          robot.keyRelease(KeyEvent.VK_TAB);
          sleep(200);
      }
      this.robot.keyPress(KeyEvent.VK_ENTER);
      sleep(200);
      this.robot.keyRelease(KeyEvent.VK_ENTER);
      sleep(5000);
      
	}
	
	private void goToFolder(){
		robot.keyPress(KeyEvent.VK_TAB);
        sleep(200);
        robot.keyRelease(KeyEvent.VK_TAB);
        this.robot.keyPress(KeyEvent.VK_ENTER);
        sleep(200);
	}
	
    private void sleep(int milliseconds){
    	try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	
	private static String specialTrim(String input){
		return input.toLowerCase().replaceAll("[^a-zA-Z0-9]+", "").trim();
	}

}
