using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;
using System.IO;
using System.Data;

using ScanManager.Common;
using System.Diagnostics;

// Example SQL Commands found in SchedulerSettings.xml file:
// _SqlJobsInProgress => "SELECT pkey, definition_name FROM fnma_measure8.scan_manager WHERE in_progress and machine_name=:machinename"
// _SqlUpdateInProgressStatus => "UPDATE fnma_measure8.scan_manager SET in_progress = :inprogress, scan_requested = :scanrequested, scan_status = :jobstatus, status_description=:statusdescription WHERE in_progress and pkey = :jobkey"

namespace ScanManager
{
    class TaskManager
    {
        protected XmlDocument _Settings;

        protected Object _lock = new Object();

        protected Boolean _IsDbBackup = false;
        protected Boolean _IsSiteDeployment = false;

        protected String _SchedulerSettingsFile = Common.MiscUtilities.AppPath() + "\\_SchedulerSettings.xml";
        protected String _SettingsFile;
        protected String _JarPath;
        protected String _JavaHome;
        protected String _ConnectionString;
        protected String _SqlNextJob;
        protected String _SqlJobsInProgress;
        protected String _SqlUpdateInProgressStatus;
        protected String _SqlNextDbBackup;

        protected ApplicationLog _AppLog = new ApplicationLog();
        protected TokenManager _tokens;

        public TaskManager()
        {
            try
            {
                if (!File.Exists(_SchedulerSettingsFile))
                    throw new FileNotFoundException(String.Format("Settings file {0} not found.", _SchedulerSettingsFile));

                LocalLog.AddLine(String.Format("Loading settings file {0}", _SchedulerSettingsFile));
                _Settings = XmlUtilities.LoadXmlFile(_SchedulerSettingsFile);
                LocalLog.AddLine("Settings file successfully loaded");
                LocalLog.AddLine("Reading system tokens");
                _tokens = new TokenManager(_Settings);
                LocalLog.AddLine("Tokens ready");

                InitializeSystem();

                LocalLog.AddLine("ScanManager is ready.");
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("ScanManager Instantiation Error: " + ex.Message);
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
                throw;
            }
        }

        public int ThreadPoolSize
        {
            get
            {
                String sPoolSize = _tokens.ResolveOptional("ThreadPool", "Size", "1");
                LocalLog.AddLine("Thread pool size requested " + sPoolSize);
                int poolSize = 1;
                if (!int.TryParse(sPoolSize, out poolSize))
                {
                    LocalLog.AddLine(String.Format("Error trying to convert [{0}] to integer.", sPoolSize));
                    poolSize = 1;
                }
                LocalLog.AddLine("Using thread pool size " + poolSize.ToString());
                return poolSize;
            }
        }

        public String ResolveToken(String tokenType, String tokenKey)
        {
            return _tokens.Resolve(tokenType, tokenKey);
        }

        public void ProcessQueue()
        {
            int JobKey = -1;
            try
            {
                // Validate the processing job and update those that are no long running.
                ValidateRunningJobs();
                // Check for any queued work
                DataRow dr = GetNextJob();
                if (dr == null) return;

                JobKey = MiscUtilities.ObjectToInteger(dr["pkey"], "No primary key is definied");
                String JobFileName = MiscUtilities.ObjectToString(dr["definition_name"], "No definition file name is defined.");
                String RawJobFileName = JobFileName;
                String lockFilename = MiscUtilities.LockFilename(RawJobFileName);
                String action_requested = MiscUtilities.ObjectToStringDefault(dr["action_requested"], "rescan");

                using (FileStream fs = new FileStream(lockFilename, FileMode.Create, FileAccess.ReadWrite, FileShare.None))
                {
                    using (StreamWriter sw = new StreamWriter(fs))
                    {
                        sw.WriteLine("While file is locked open, processing thread is running.  Seeing this message means the thread may not be running, please delete this file.");
                        if (_IsDbBackup)
                        {
                            BackupCastDatabase(JobKey, JobFileName, RawJobFileName);
                        }
                        else
                        {
                            String message = "Processing";
                            switch (action_requested.ToLower())
                            {
                                case "publish":
                                    JobFileName = _tokens.Resolve("Scheduler", "PublishDefinition", "No publish definition defined in the settings file.  Please add a Tokens/Scheduler PublishDefinition");
                                    message = "Publishing";
                                    break;
                                case "empty":
                                case "rescan":
                                    message = "Processing";
                                    break;
                                case "onboard":
                                    JobFileName = _tokens.Resolve("Scheduler", "OnBoardDefinition", "No onboard definition defined in the settings file.  Please add a Tokens/Scheduler OnBoardDefinition");
                                    message = "OnBoarding";
                                    break;
                                default:
                                    throw new Exception(String.Format("{0} is not a valid action request.", action_requested));
                            }

                            // Kick off the definition
                            RunDefinition(JobKey, JobFileName, RawJobFileName, message);
                        }
                    }
                    fs.Close();
                }
                File.Delete(lockFilename);
            }
            catch (Exception ex)
            {
                lock (_lock)
                {
                    LocalLog.AddLine("Queue Error: " + ex.Message);
                }
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);

                if (JobKey > 0)
                {
                    Dictionary<String, Object> sqlParameters = new Dictionary<string, object>();
                    sqlParameters.Add(":jobkey", JobKey);
                    sqlParameters.Add(":jobstatus", "Error");
                    sqlParameters.Add(":inprogress", false);
                    sqlParameters.Add(":scanrequested", false);
                    String message = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message);
                    if (message.Length > 99) message = message.Substring(0, 99);
                    sqlParameters.Add(":statusdescription", message);
                    SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateInProgressStatus, sqlParameters);
                }
            }
        }

        protected void InitializeSystem()
        {
            LocalLog.AddLine("Reading system SQL queries..");
            String Message = "Missing the {0} {1} SQL query. Please add the query into the settings file under SchedulerSettings/Tokens/{0} {1}";

            _SqlNextJob = _tokens.Resolve("Scheduler", "NextJob", Message);
            _SqlJobsInProgress = _tokens.Resolve("Scheduler", "JobsInProgress", Message);
            _SqlUpdateInProgressStatus = _tokens.Resolve("Scheduler", "UpdateInProgress", Message);
            _SqlNextDbBackup = _tokens.Resolve("Scheduler", "NextDbBackup", Message);

            String connID = _tokens.Resolve("Scheduler", "ConnectionID", "Missing the connection ID for the database server.  Please add the ConnectionID to the settings file under SchedulerSettings/Tokens/{0} {1}");
            LocalLog.AddLine("Reading connection information..");
            _ConnectionString = XmlUtilities.RequiredAttribute(_Settings, "//Connections/Connection[@ID='" + connID + "']", "ConnectionString", "No " + connID + " connection information found.");

            LocalLog.AddLine("Reading location of JAVA..");
            _JavaHome = _tokens.Resolve("JavaHome", "Path", "Missing path to JAVA bin directory. Please add the path to SchedulerSettings/Tokens/JavaHome Path");
            if (!_JavaHome.EndsWith("\\")) _JavaHome += "\\";
            LocalLog.AddLine(String.Format("JAVA location is {0}", _JavaHome));

            _JarPath = _tokens.Resolve("PieJar", "Path", "Missing path to PIE JAR. Please add the path to SchedulerSettings/Tokens/PieJar Path");
            _SettingsFile = _tokens.Resolve("PieSettings", "Path", "Missing path to PIE Settings file. Please add the path to SchedulerSettings/Tokens/PieSettings Path");
            if (!File.Exists(_JarPath))
            {
                String errorMessage = String.Format("JAR file {0} not found.", _JarPath);
                FileNotFoundException ex = new FileNotFoundException(errorMessage);
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
                throw ex;
            }
            else
            {
                LocalLog.AddLine("Using PIE JAR found at " + _JarPath);
            }

            if (!File.Exists(_SettingsFile))
            {
                FileNotFoundException ex = new FileNotFoundException(String.Format("PIE Settings file {0} not found.", _SettingsFile));
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
                throw ex;
            }
            else
            {
                LocalLog.AddLine("Using PIE Settings file from " + _SettingsFile);
            }

            LocalLog.AddLine("Reading service mode..");
            String ServiceMode = _tokens.ResolveOptional("Scheduler", "ServiceMode", "Normal").Replace(" ", "");
            if (ServiceMode.IndexOf("DatabaseBackup", StringComparison.CurrentCultureIgnoreCase) != -1)
            {
                _IsDbBackup = true;
                LocalLog.AddLine("*** Running as database backup machine.");
            }
            else if (ServiceMode.IndexOf("|SiteDeployment|", StringComparison.CurrentCultureIgnoreCase) != -1)
            {
                _IsSiteDeployment = true;
                LocalLog.AddLine("*** Running as CED site delpoyment machine.");
            }
            else
            {
                LocalLog.AddLine("Running in normal mode.");
            }
        }

        protected void ValidateRunningJobs()
        {
            Dictionary<String, Object> sqlParameters = new Dictionary<String, Object>();
            sqlParameters.Add(":machinename", Environment.MachineName);

            // Pull the jobs running on this machine.
            DataTable dt = SqlUtilities.GetData(_ConnectionString, _SqlJobsInProgress, sqlParameters);
            if ((dt != null) && (dt.Rows != null) && (dt.Rows.Count > 0))
            {
                sqlParameters.Add(":inprogress", false);
                sqlParameters.Add(":scanrequested", false);
                sqlParameters.Add(":jobstatus", "Error");
                sqlParameters.Add(":statusdescription", "See activity log for details.");
                sqlParameters.Add(":pkey", -1);
                for (int i = 0; i < dt.Rows.Count; i++)
                {
                    try
                    {
                        if (!MiscUtilities.isAlive(dt.Rows[i]["definition_name"].ToString()))
                        {
                            sqlParameters[":pkey"] = MiscUtilities.ObjectToInteger(dt.Rows[i]["pkey"], "No primary key defined.");
                            SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateInProgressStatus, sqlParameters);
                        }
                    }
                    catch (Exception ex)
                    {
                        LocalLog.AddLine("Error while updating in progress job status.  Error message was: " + ex.Message);
                        _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
                    }
                }
            }
        }

        protected DataRow GetNextJob()
        {
            try
            {
                Dictionary<String, Object> sqlParameters = new Dictionary<String, Object>();
                sqlParameters.Add(":machinename", Environment.MachineName);

                DataTable dt = SqlUtilities.GetData(_ConnectionString, _SqlNextJob, sqlParameters);
                if ((dt != null) && (dt.Rows != null) && (dt.Rows.Count > 0))
                {
                    return dt.Rows[0];
                }
                return null;
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Error while getting next job from queue.  Error message was: " + ex.Message);
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
            }
            return null;
        }

        protected Boolean RunDefinition(int JobKey, String JobFileName, String RawJobFileName, String processingMessage)
        {
            Dictionary<String, Object> sqlParameters = new Dictionary<string, object>();
            sqlParameters.Add(":inprogress", true);
            sqlParameters.Add(":scanrequested", true);
            sqlParameters.Add(":jobstatus", processingMessage);
            sqlParameters.Add(":statusdescription", String.Format("Started: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now));
            sqlParameters.Add(":machinename", Environment.MachineName);
            sqlParameters.Add(":jobkey", JobKey);

            try
            {
                SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateInProgressStatus, sqlParameters);
                // Shell to the JAVA program and run it.
                RunJava(JobKey, JobFileName, RawJobFileName);

                // Job finished update record.
                sqlParameters[":inprogress"] = false;
                sqlParameters[":scanrequested"] = false;
                sqlParameters[":jobstatus"] = "Completed";
                sqlParameters[":statusdescription"] = String.Format("Completed: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
                sqlParameters[":machinename"] = null;

                // Error out this queue item and rest for this application.
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Code Scan Error: " + ex.Message);
                LocalLog.AddLine(ex);

                String message = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message);
                if (message.Length > 99) message = message.Substring(0, 99);
                sqlParameters[":statusdescription"] = message;
                sqlParameters[":inprogress"] = false;
                sqlParameters[":scanrequested"] = false;
                sqlParameters[":jobstatus"] = "Error";
                sqlParameters[":machinename"] = null;
            }
            SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateInProgressStatus, sqlParameters);
            return false;
        }

        protected void RunJava(int JobKey, string JobFileName, string RawJobFileName)
        {
            String args = String.Format("-s {0} -d {1} JobKey={2} DefinitionFile={3}", MiscUtilities.WrapSpaces(_SettingsFile), MiscUtilities.WrapSpaces(JobFileName), JobKey, RawJobFileName);
            using (Process clientProcess = new Process())
            {
                clientProcess.StartInfo.UseShellExecute = false;
                clientProcess.StartInfo.RedirectStandardOutput = true;
                clientProcess.StartInfo.RedirectStandardError = true;
                clientProcess.StartInfo.CreateNoWindow = true;
                clientProcess.StartInfo.WorkingDirectory = MiscUtilities.AppParentPath();
                clientProcess.StartInfo.FileName = MiscUtilities.WrapSpaces(_JavaHome + "java.exe");
                clientProcess.StartInfo.Arguments = @"-jar " + MiscUtilities.WrapSpaces(_JarPath) + " " + args;
                lock (_lock)
                {
                    LocalLog.AddLine(String.Format("Command line: {0} {1}", MiscUtilities.WrapSpaces(_JavaHome + "java.exe"), @"-jar " + MiscUtilities.WrapSpaces(_JarPath) + " " + args));
                }
                clientProcess.Start();
                string output = clientProcess.StandardOutput.ReadToEnd();
                string errorOutput = clientProcess.StandardError.ReadToEnd();
                clientProcess.WaitForExit();
                int exitcode = clientProcess.ExitCode;
                LocalLog.AddLine("Console Output: " + System.Environment.NewLine + output);
                if (exitcode > 0)
                {
                    LocalLog.AddLine("ERROR OUTPUT: " + System.Environment.NewLine + errorOutput);
                    throw new Exception(String.Format("Run JAVA error ExitCode {0} running {1} {2}", exitcode, clientProcess.StartInfo.FileName, clientProcess.StartInfo.Arguments));
                }
            }
        }

        protected Boolean BackupCastDatabase(int JobKey, String JobFileName, String RawJobFileName)
        {
            Dictionary<String, Object> sqlParameters = new Dictionary<string, object>();
            sqlParameters.Add(":inprogress", true);
            sqlParameters.Add(":scanrequested", true);
            sqlParameters.Add(":jobstatus", "Ready");
            sqlParameters.Add(":statusdescription", String.Format("Backup Started: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now));
            sqlParameters.Add(":machinename", Environment.MachineName);
            sqlParameters.Add(":jobkey", JobKey);

            JobFileName = _tokens.Resolve("Scheduler", "BackupDatabase", "Missing the Scheduler/BackupDatabase attribute with the name of the backup definition. SQL query. Please add the name of the backup definition to the settings file under SchedulerSettings/Tokens/Scheduler BackupDatabase");
            // Look for backup database request
            DataTable dt = SqlUtilities.GetData(_ConnectionString, _SqlNextDbBackup);
            if ((dt == null) || (dt.Rows.Count == 0)) return false;

            for (int i = 0; i < dt.Rows.Count; i++)
            {
                try
                {
                    JobKey = MiscUtilities.ObjectToInteger(dt.Rows[0]["pkey"], "No primary key is definied");
                    sqlParameters[":jobkey"] = JobKey;
                    RunJava(JobKey, JobFileName, RawJobFileName);

                    // Job finished update record.
                    sqlParameters[":jobstatus"] = "Backup Completed";
                    sqlParameters[":statusdescription"] = String.Format("Backup Completed: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
                }
                catch (Exception ex)
                {
                    LocalLog.AddLine("Database Backup Error: " + ex.Message);
                    sqlParameters[":jobstatus"] = "Error";
                    String message = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message);
                    if (message.Length > 99) message = message.Substring(0, 99);
                    sqlParameters[":statusdescription"] = message;
                }
                SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateInProgressStatus, sqlParameters);
            }
            return false;
        }
    }
}
