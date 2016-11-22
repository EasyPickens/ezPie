package consoleapp;

import java.io.IOException;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.fanniemae.devtools.pie.JobManager;

public class RunPie {

	public static void main(String[] args) {
		runJobManager();
	}

	protected static void runJobManager() {
		// String sSettings = "C:\\Developers\\Test\\_Settings.xml";
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
		// String job = "clmDefinition.xml";
		// String job = "clm.xml";
		// String job = "FileDirectory_Test.xml";
		// String job = "AIP_AFP_Export.xml";
		// String job = "Backup_Fnma_Apps.xml";
		// String job = "Monthly_CQA_Report.xml";
		// String job = "REPORT_EFP_YTD.xml";
		// String job = "VersionFile_Test.xml";
		// String job = "Evas_Rescan.xml";
		// String job = "AIP_OnBoard.xml";
		// String job = "SVN_Test.xml";
		// String job = "Rescan_EPV.xml";
		// String job = "Test_Actions.xml";
		// String job = "dbsimple.xml";
		// String job = "SqlExecute.xml";
		// String job = "SMDU.xml";
		// String job = "PEWholeLoanB2B.xml";
		// String job = "SharedTest.xml";
		// String job = "SharedA.xml";
		// String job="DatabaseBackup.xml";
		// String job="RescanTest.xml";
		// String job= "ResultSet.xml";
		// String job="RunCmdError.xml";
		// String job="LogFilenameTest.xml";
		// String job="EmptyScanTest.xml";
		
		//String job="Add_RescanTables.xml";
		
		//String job="Scan_Limiter.xml";
		
		// String job="Backup_Tables.xml";
		
        // String job="BranchingTest.xml";
	    // String job="AIP_OnBoard.xml";
		// String job="EPV.xml";
		// String job="RemoveStarRisk.xml";
		// String job="BlankDefinition.xml";
		// String job = "If_Test.xml";
        // String job = "Xml_Test.xml";
        // String job = "Snapshot_Date.xml";
		// String job = "RemoveStarRiskRating.xml";
		String job = "DeleteEmpty.xml";
		String logFilename = null;
		try {
			System.out.println("Initializing PIE JobManager");
			JobManager jobManager = new JobManager(sSettings, job, null);
			logFilename = jobManager.getLogFilename();
			viewlog(logFilename);
			System.out.println("Running job definition " + job);
			jobManager.runJob();
			System.out.println("Job defintion processing completed.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// viewlog(logFilename);
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
