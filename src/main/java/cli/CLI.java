package cli;

import java.io.IOException;
import java.util.List;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fanniemae.devtools.pie.JobManager;

public class CLI {
	private String[] _args = null;
	private Options _options = new Options();

	private String _settings;
	private String _job;
	
	public CLI(String[] args) {
		this._args = args;

		_options.addOption("h", "help", false, "Show help.");
		_options.addOption("s", "settings", true, "Location of xml settings file.");
		_options.addOption("d", "definition", true, "Location of xml definition file to run.");

	}

	public void parse() {
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(_options, _args);

			if (cmd.hasOption("h"))
				help();

			if (cmd.hasOption("s")) {
				this._settings = cmd.getOptionValue("s");
			} else {
				System.out.println("Missing -s option. Please provide path to xml settings file.");
				help();
			}
			
			if (cmd.hasOption("d")) {
				this._job = cmd.getOptionValue("d");
			} else {
				System.out.println("Missing -d option. Please provide path to xml definition file.");
				help();
			}
			
			runJobManager(cmd);

		} catch (ParseException e) {
			
			help();
		}
	}
	
	protected void runJobManager(CommandLine cmd){
		String logFilename = null;
		try {
			System.out.println("Initializing PIE JobManager");
			JobManager jobManager = new JobManager(_settings, _job);
			List<String> args = cmd.getArgList();
			for(int i = 0; i < args.size(); i++){
				String[] keyValuePair = args.get(i).split("=");
				jobManager.getSession().addToken("Local", keyValuePair[0], keyValuePair[1]);
			}
			logFilename = jobManager.getLogFilename();
			viewlog(logFilename);
			System.out.println("Running job definition " + _job);
			jobManager.runJob();
			System.out.println("Job definition processing completed.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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
	
	protected void help() {
		// This prints out some help
		HelpFormatter formater = new HelpFormatter();

		formater.printHelp("Main", _options);
		System.exit(0);
	}
}
