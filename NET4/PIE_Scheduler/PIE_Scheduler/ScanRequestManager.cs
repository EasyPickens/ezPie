using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;
using System.IO;
using System.Data;
using System.Diagnostics;

using ScanManager.Common;

namespace ScanManager
{
    class ScanRequestManager
    {
        protected Boolean _IsDbBackup = false;
        protected XmlDocument _Settings;
        protected String _SettingsFile;
        protected String _JarPath;
        protected String _JavaHome;

        protected String _ConnectionString;
        protected String _SqlNextJob;
        protected String _SqlErrorOutInProgress;
        protected String _SqlUpdateStatus;
        protected String _SqlJobFinished;
        protected String _SqlJobStarted;

        protected int _JobKey = -1;
        protected String _JobFileName;

        protected String _errorMessage;
        protected ApplicationLog _AppLog = new ApplicationLog();

        public ScanRequestManager(String SettingsFile, String JarPath)
        {
            try
            {
                if (!File.Exists(SettingsFile))
                {
                    _errorMessage = String.Format("Settings file {0} not found.", SettingsFile);
                    throw new FileNotFoundException(_errorMessage);
                }
                else
                {
                    LocalLog.AddLine("Using " + SettingsFile);
                }
                if (!File.Exists(JarPath))
                {
                    _errorMessage = String.Format("JAR file {0} not found.", JarPath);
                    throw new FileNotFoundException(_errorMessage);
                }
                else
                {
                    LocalLog.AddLine("Using " + JarPath);
                }
                _SettingsFile = SettingsFile;
                _JarPath = JarPath;
                _Settings = XmlUtilities.LoadXmlFile(_SettingsFile);
                ReadQueries();
                LocalLog.AddLine("ScanManager is ready.");
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("ScanManager Instantiation Error: " + ex.Message);
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
                throw;
            }
        }

        public Boolean ProcessQueue()
        {
            try
            {
                if (this._IsDbBackup)
                {
                    return BackupCastDatabase();
                }
                else
                {
                    return RunScan();
                }

            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Queue Error: " + ex.Message);
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);
            }
            return false;
        }

        protected Boolean BackupCastDatabase()
        {
            Dictionary<String, Object> aParams = new Dictionary<string, object>();
            aParams.Add(":jobkey", _JobKey);
            aParams.Add(":jobstatus", "Ready");
            aParams.Add(":statusdescription", "Ready");

            try
            {
                // Look for the next rescan request.
                DataTable requests = SqlUtilities.GetData(_ConnectionString, "SELECT pkey, definition_name, scan_status, dbprefix FROM fnma_measure8.scan_manager WHERE in_progress and scan_status='Backup Database'");
                if ((requests == null) || (requests.Rows.Count == 0)) return false;

                _JobKey = (int)requests.Rows[0]["pkey"];
                _JobFileName = (String)requests.Rows[0]["definition_name"];
                aParams[":jobkey"] = _JobKey;
                _JobFileName = "DatabaseBackup.xml";

                // Shell to the JAVA program and run it.
                RunJava();

                // Job finished update record.
                aParams[":jobstatus"] = "Backup Completed";
                aParams[":statusdescription"] = String.Format("Backup Completed: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Database Backup Error: " + ex.Message);
                aParams[":jobstatus"] = "Error";
                aParams[":statusdescription"] = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message).Substring(0, 99);
            }

            SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateStatus, aParams);
            return false;
        }

        protected Boolean RunScan()
        {
            Dictionary<String, Object> aParams = new Dictionary<string, object>();
            aParams.Add(":jobkey", _JobKey);
            aParams.Add(":jobstatus", "Ready");
            aParams.Add(":statusdescription", "Ready");

            try
            {
                // Set any definitions left from previous run 'in progress' to error.
                SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlErrorOutInProgress);

                // Look for the next rescan request.
                DataTable requests = SqlUtilities.GetData(_ConnectionString, _SqlNextJob);
                if ((requests == null) || (requests.Rows.Count == 0)) return false;

                _JobKey = (int)requests.Rows[0]["pkey"];
                _JobFileName = (String)requests.Rows[0]["definition_name"];
                aParams[":jobkey"] = _JobKey;

                // Run the definition and wait till it finishes.
                aParams[":jobstatus"] = "Processing";
                aParams[":statusdescription"] = String.Format("Started: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
                SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateStatus, aParams);

                // Shell to the JAVA program and run it.
                RunJava();

                // Job finished update record.
                aParams[":jobstatus"] = "Completed";
                aParams[":statusdescription"] = String.Format("Completed: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Code Scan Error: " + ex.Message);
                aParams[":jobstatus"] = "Error";
                aParams[":statusdescription"] = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message).Substring(0, 99);
            }
            SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlJobFinished, aParams);
            return false;
        }

        protected void RunJava()
        {
            String args = String.Format("-s {0} -d {1} JobKey={2}", _SettingsFile, _JobFileName, _JobKey);
            using (Process clientProcess = new Process())
            {
                clientProcess.StartInfo.UseShellExecute = false;
                clientProcess.StartInfo.RedirectStandardOutput = true;
                clientProcess.StartInfo.RedirectStandardError = true;
                clientProcess.StartInfo.CreateNoWindow = true;
                clientProcess.StartInfo.WorkingDirectory = MiscUtilities.AppParentPath();
                clientProcess.StartInfo.FileName = WrapSpaces(_JavaHome + "java.exe");
                clientProcess.StartInfo.Arguments = @"-jar " + WrapSpaces(_JarPath) + " " + args;
                LocalLog.AddLine(String.Format("Command line: {0} {1}", WrapSpaces(_JavaHome + "java.exe"), @"-jar " + WrapSpaces(_JarPath) + " " + args));
                clientProcess.Start();
                string output = clientProcess.StandardOutput.ReadToEnd();
                clientProcess.WaitForExit();
                int exitcode = clientProcess.ExitCode;
                if (exitcode > 0)
                    throw new Exception(String.Format("Run JAVA error ExitCode {0} running {1} {2}", exitcode, clientProcess.StartInfo.FileName, clientProcess.StartInfo.Arguments));
            }
        }

        protected void ReadQueries()
        {
            LocalLog.AddLine("Confirming LogPath..");
            String logPath = OptionalAttribute("//Configuration", "LogPath");
            if (!String.IsNullOrEmpty(logPath))
            {
                // Check to see if it Exists
                if (!Directory.Exists(logPath))
                    throw new Exception(String.Format("Configured LogPath directory {0} does not exist.", logPath));
                // Check to see if you can write to it
                if (!logPath.EndsWith(Path.DirectorySeparatorChar.ToString())) logPath += Path.DirectorySeparatorChar;
                String filename = logPath + "test.tmp";
                try
                {
                    using (FileStream fstream = new FileStream(filename, FileMode.Create))
                    {
                        using (TextWriter writer = new StreamWriter(fstream))
                        {
                            writer.WriteLine("Verify rights.");
                            writer.Close();
                        }
                        fstream.Close();
                    }
                    File.Delete(filename);
                }
                catch (Exception ex)
                {
                    throw new Exception(String.Format("Do not have write to create/write log files in {0}. " + ex.Message, logPath), ex);
                }
                LocalLog.AddLine(String.Format("LogPath set to {0}", logPath));
            }
            LocalLog.AddLine("Reading system SQL queries..");
            String Message = "Missing the {0} SQL query. Please add the query into the settings file under Settings/SelServiceScan/{0}.";
            _SqlNextJob = ReadAttribute("//SelfServiceScan/NextJob", "SqlQuery", Message);
            _SqlErrorOutInProgress = ReadAttribute("//SelfServiceScan/ErrorOldInProgress", "SqlQuery", Message);
            _SqlUpdateStatus = ReadAttribute("//SelfServiceScan/UpdateStatus", "NetSqlQuery", Message);
            _SqlJobFinished = ReadAttribute("//SelfServiceScan/FinishedProcessing", "NetSqlQuery", Message);
            _SqlJobStarted = ReadAttribute("//SelfServiceScan/StartProcessing", "NetSqlQuery", Message);

            LocalLog.AddLine("Reading connection information..");
            _ConnectionString = ReadAttribute("//Connections/Connection[@ID='NetScanManager']", "ConnectionString", "No ScanManager connection information found.");

            LocalLog.AddLine("Reading location of JAVA..");
            _JavaHome = ReadAttribute("//SelfServiceScan/JavaHome", "Path", "Missing path to JAVA bin directory. Please add the path to Settings/SelServiceScan/JavaHome");
            if (!_JavaHome.EndsWith("\\")) _JavaHome += "\\";
            LocalLog.AddLine(String.Format("JAVA location is {0}", _JavaHome));

            LocalLog.AddLine("Reading service mode..");
            String BackupMachine = OptionalAttribute("//SelfServiceScan", "DatabaseBackupServer");
            if (!String.IsNullOrEmpty(BackupMachine) && "True".Equals(BackupMachine, StringComparison.CurrentCultureIgnoreCase))
            {
                _IsDbBackup = true;
                LocalLog.AddLine("*** Running as database backup machine: True");
            }
            else
            {
                LocalLog.AddLine("Running as database backup machine: false");
            }
        }

        protected String OptionalAttribute(String XPath, String Attr)
        {
            XmlElement ele = (XmlElement)_Settings.SelectSingleNode(XPath);
            if ((ele == null) || !ele.HasAttribute(Attr))
                return "";

            return ele.GetAttribute(Attr);
        }

        protected String ReadAttribute(String XPath, String Attr, String ErrorMessage)
        {
            XmlElement ele = (XmlElement)_Settings.SelectSingleNode(XPath);
            if ((ele == null) || !ele.HasAttribute(Attr))
                throw new Exception(String.Format(ErrorMessage, Attr));

            return ele.GetAttribute(Attr);
        }

        protected String WrapSpaces(String value)
        {
            if (!value.Contains(" ")) return value;

            return String.Format("\"{0}\"", value);
        }
    }
}
