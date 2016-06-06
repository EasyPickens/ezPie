package consoleapp;

import java.io.IOException;

import com.fanniemae.devtools.pie.JobManager;

public class RunPie {

	public static void main(String[] args) {
		runJobManager();
	}

	protected static void runJobManager() {
		String sSettings = "C:\\Developers\\Code\\TestDirectory\\_Settings.xml";
		// String job = "test.xml";
		// String job = "CMFT_Monthly_Scores.xml";
		// String job = "CMFT_Monthly_v2.xml";
		// String job = "HighlightDownload.xml";
		// String job = "ZipTest.xml";
		// String job = "AIP_All_Rules.xml";
		// String job = "GitTest.xml";
		// String job = "GitPIE.xml";
		// String job = "Git_PAM.xml";
		// String job = "SVN_Test.xml";
		// String job = "WireGateway.xml";
		// String job = "AIP_Rules_Criteria.xml";
		// String job = "JoinTest.xml";
		// String job = "ReportTest.xml";
		String job = "FileDirectory_Test.xml";
		// String job = "AIP_AFP_Export.xml";
		// String job = "Backup_Fnma_Apps.xml";
		// String job = "Monthly_CQA_Report.xml";
		String logFilename = null;
		try {
			System.out.println("Initializing PIE JobManager");
			JobManager jobManager = new JobManager(sSettings, job);
			logFilename = jobManager.getLogFilename();
			viewlog(logFilename);
			System.out.println("Running job definition " + job);
			jobManager.runJob();
			System.out.println("Job defintion processing completed.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		//viewlog(logFilename);
	}

	protected static void viewlog(String logFilename) {
		if (logFilename == null)
			return;
		try {
			Runtime.getRuntime().exec(new String[] { "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", logFilename });
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
