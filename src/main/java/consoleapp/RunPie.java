package consoleapp;

import java.io.IOException;

import com.fanniemae.devtools.pie.JobManager;

public class RunPie {

	public static void main(String[] args) {
		runJobManager();
	}

	protected static void runJobManager() {
		String sSettings = "C:\\Developers\\Code\\TestDirectory\\_Settings.xml";
		// String sjob = "test.xml";
		// String sjob = "CMFT_Monthly_Scores.xml";
		//String sjob = "CMFT_Monthly_v2.xml";
		String sjob = "HighlightDownload.xml";
		// String sjob = "JoinTest.xml";
		// String sjob = "ReportTest.xml";
		System.out.println("Initializing job manager");
		JobManager jobManager = new JobManager(sSettings, sjob);
		String logFilename = jobManager.getLogFilename();
		System.out.println("Running job");
		jobManager.runJob();
		System.out.println("Job request completed.");
		viewlog(logFilename);		
	}
	
	protected static void viewlog(String logFilename) {
		try {
			Runtime.getRuntime().exec(new String[] {"C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",logFilename});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
