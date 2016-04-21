package consoleapp;

import com.fanniemae.devtools.pie.JobManager;

public class RunPie {

	public static void main(String[] args) {
		runJobManager();
	}

	protected static void runJobManager() {
		String sSettings = "C:\\Developers\\Code\\TestDirectory\\_Settings.xml";
		// String sjob = "test.xml";
		// String sjob = "CMFT_Monthly_Scores.xml";
		String sjob = "CMFT_Monthly_v2.xml";
		// String sjob = "JoinTest.xml";
		// String sjob = "ReportTest.xml";
		JobManager oJob = new JobManager(sSettings, sjob);
		oJob.runJob();
	}

}
