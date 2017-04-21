/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;
using System.IO;
using System.Data;
using System.Diagnostics;

using ScanManager.Common;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-11
 * 
 */

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
        protected String _SqlTakeNextJob;

        protected int _JobKey = -1;
        protected String _JobFileName;
        protected String _RawJobFileName;

        protected String _errorMessage;
        protected ApplicationLog _AppLog = new ApplicationLog();

        protected Dictionary<String, Dictionary<String, String>> _tokens = new Dictionary<string, Dictionary<string, string>>();

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
                    Dictionary<String,Object> aParams = new Dictionary<String,Object>();
                    aParams.Add(":machinename", Environment.MachineName);

                    // Set any definitions left in_progress from previous run to error. -- turned off, to test.
                    SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlErrorOutInProgress, aParams);

                    // Update next ready record with machine name
                    int gotOne = SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlNextJob, aParams);
                    if (gotOne == 0) return false;

                    // Check for next request
                    DataTable requests = SqlUtilities.GetData(_ConnectionString, _SqlNextJob, aParams);
                    if ((requests == null) || (requests.Rows.Count == 0))
                    {
                        // Ready the next application in the automated queue
                        DataTable next_automated_queue = SqlUtilities.GetData(_ConnectionString, "SELECT scan_manager_pkey, code_loc_type, code_loc_url, version_name FROM fnma_measure8.automated_queue WHERE ready and scan_manager_pkey > 0 ORDER BY scan_order, id");
                        if ((next_automated_queue != null) && (next_automated_queue.Rows.Count > 0))
                        {
                            int updatedCount = 0;
                            int lastPkey = -1;
                            foreach (DataRow dr in next_automated_queue.Rows) {
                                int scanManagerPkey = ObjectToInteger(dr["scan_manager_pkey"], "No scan_manager_pkey is definied");
                                if (scanManagerPkey == lastPkey) continue;

                                lastPkey = scanManagerPkey;
                                try
                                {
                                    int codeLocationType = ObjectToInteger(dr["code_loc_type"], "No code_loc_type definied");
                                    String codeLocationUrl = ObjectToString(dr["code_loc_url"], "No code_loc_url defined.");
                                    String versionName = ObjectToString(dr["version_name"], "No version_name defined.");
                                    String request_date = String.Format("{0:yyyy-MM-dd HH:mm:ss}", DateTime.Now);

                                    // Queue the next rescan
                                    updatedCount = SqlUtilities.ExcecuteNonQuery(_ConnectionString, String.Format("UPDATE fnma_measure8.scan_manager SET scan_requested=true, request_date='{0}', scan_status='Queued', code_url='{1}', code_version='{2}', action_requested='rescan', code_location_type={3} WHERE pkey={4} and machine_name is null and in_progress=false and scan_requested=false and scan_status='Completed'", request_date, codeLocationUrl, versionName, codeLocationType, scanManagerPkey));
                                }
                                catch (Exception exAutomated)
                                {
                                    String error = exAutomated.Message;
                                    // Error out this queue item and rest for this application.
                                    SqlUtilities.ExcecuteNonQuery(_ConnectionString, String.Format("UPDATE fnma_measure8.automated_queue SET ready=false, status='ERROR' WHERE ready and scan_manager_pkey={0}", scanManagerPkey));
                                }
                                if (updatedCount > 0) break;
                            }
                        }
                        //if ((next_automated_queue != null) && (next_automated_queue.Rows.Count > 0))
                        //{
                        //    int scanManagerPkey = ObjectToInteger(next_automated_queue.Rows[0]["scan_manager_pkey"], "No scan_manager_pkey is definied");
                        //    try
                        //    {
                        //        int codeLocationType = ObjectToInteger(next_automated_queue.Rows[0]["code_loc_type"], "No code_loc_type definied");
                        //        String codeLocationUrl = ObjectToString(next_automated_queue.Rows[0]["code_loc_url"], "No code_loc_url defined.");
                        //        String versionName = ObjectToString(next_automated_queue.Rows[0]["version_name"], "No version_name defined.");
                        //        String request_date = String.Format("{0:yyyy-MM-dd HH:mm:ss}", DateTime.Now);

                        //        // Queue the next rescan
                        //        SqlUtilities.ExcecuteNonQuery(_ConnectionString, String.Format("UPDATE fnma_measure8.scan_manager SET scan_requested=true, request_date='{0}', scan_status='Queued', code_url='{1}', code_version='{2}', action_requested='rescan', code_location_type={3}, machine_name='{5}' WHERE pkey={4} and machine_name is null", request_date, codeLocationUrl, versionName, codeLocationType, scanManagerPkey, Environment.MachineName));
                        //    }
                        //    catch (Exception exAutomated)
                        //    {
                        //        String error = exAutomated.Message;
                        //        // Error out this queue item and rest for this application.
                        //        SqlUtilities.ExcecuteNonQuery(_ConnectionString, String.Format("UPDATE fnma_measure8.automated_queue SET ready=false, status='ERROR' WHERE ready and scan_manager_pkey={0}", scanManagerPkey));
                        //    }
                        //}
                        return false;
                    }


                    _JobKey = ObjectToInteger(requests.Rows[0]["pkey"], "No primary key is definied");
                    _JobFileName = ObjectToString(requests.Rows[0]["definition_name"], "No definition file name is defined.");
                    _RawJobFileName = _JobFileName;

                    // Until the UI is updated, defaulting to 'rescan'
                    //String action_requested = ObjectToString(requests.Rows[0]["action_requested"], "Action_request value is null.  Typical actions include Publish, Rescan, or Onboard.");
                    String action_requested = ObjectToStringDefault(requests.Rows[0]["action_requested"], "rescan");

                    String message = "Scanning";
                    switch (action_requested.ToLower())
                    {
                        case "publish":
                            _JobFileName = ReadTokenValue("SelfServiceScan", "PublishDefinition", "No publish definition defined in the settings file.  Please add a Tokens/SelfServiceScan PublishDefinition");
                            message = "Publishing";
                            break;
                        case "empty":
                        case "rescan":
                            message = "Processing";
                            break;
                        case "onboard":
                            _JobFileName = ReadTokenValue("SelfServiceScan", "OnBoardDefinition", "No onboard definition defined in the settings file.  Please add a Tokens/SelfServiceScan OnBoardDefinition");
                            message = "OnBoarding";
                            break;
                        default:
                            throw new Exception(String.Format("{0} is not a valid action request.", action_requested));
                    }
                    return RunDefinition(message);
                }
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Queue Error: " + ex.Message);
                _AppLog.WriteEntry(ex.StackTrace, System.Diagnostics.EventLogEntryType.Error);

                if (_JobKey > 0)
                {
                    Dictionary<String, Object> aParams = new Dictionary<string, object>();
                    aParams.Add(":jobkey", _JobKey);
                    aParams.Add(":jobstatus", "Error");
                    String message = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message);
                    if (message.Length > 99) message = message.Substring(0, 99);
                    aParams.Add(":statusdescription", message);
                    SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlJobFinished, aParams);
                }
            }
            return false;
        }

        protected Boolean BackupCastDatabase()
        {
            Dictionary<String, Object> sqlParameters = new Dictionary<string, object>();
            sqlParameters.Add(":jobkey", _JobKey);
            sqlParameters.Add(":jobstatus", "Ready");
            sqlParameters.Add(":statusdescription", "Ready");

            try
            {
                // Look for the next rescan request.
                DataTable requests = SqlUtilities.GetData(_ConnectionString, "SELECT pkey, definition_name, scan_status, dbprefix FROM fnma_measure8.scan_manager WHERE in_progress and scan_status='Backup Database'");
                if ((requests == null) || (requests.Rows.Count == 0)) return false;

                _JobKey = (int)requests.Rows[0]["pkey"];
                _JobFileName = (String)requests.Rows[0]["definition_name"];
                sqlParameters[":jobkey"] = _JobKey;
                _JobFileName = "DatabaseBackup.xml";

                // Shell to the JAVA program and run it.
                RunJava();

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

            SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateStatus, sqlParameters);
            return false;
        }

        protected Boolean RunDefinition(String processingMessage)
        {
            Dictionary<String, Object> sqlParameters = new Dictionary<string, object>();
            sqlParameters.Add(":jobkey", _JobKey);
            sqlParameters.Add(":jobstatus", "Ready");
            sqlParameters.Add(":statusdescription", "Ready");

            try
            {
                sqlParameters[":jobkey"] = _JobKey;

                // Run the definition and wait till it finishes.
                sqlParameters[":jobstatus"] = processingMessage;
                sqlParameters[":statusdescription"] = String.Format("Started: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
                SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateStatus, sqlParameters);

                // Shell to the JAVA program and run it.
                RunJava();

                // Job finished update record.
                sqlParameters[":jobstatus"] = "Completed";
                sqlParameters[":statusdescription"] = String.Format("Completed: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
                // Error out this queue item and rest for this application.
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Code Scan Error: " + ex.Message);
                sqlParameters[":jobstatus"] = "Error";
                String message = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message);
                if (message.Length > 99) message = message.Substring(0, 99);
                sqlParameters[":statusdescription"] = message;
                SqlUtilities.ExcecuteNonQuery(_ConnectionString, String.Format("UPDATE fnma_measure8.automated_queue SET ready=false, status='ERROR' WHERE ready and scan_manager_pkey={0}", _JobKey));
            }
            SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlJobFinished, sqlParameters);
            return false;
        }

        //protected Boolean RunScan()
        //{
        //    Dictionary<String, Object> aParams = new Dictionary<string, object>();
        //    aParams.Add(":jobkey", _JobKey);
        //    aParams.Add(":jobstatus", "Ready");
        //    aParams.Add(":statusdescription", "Ready");
        //    aParams.Add(":machinename", Environment.MachineName);

        //    try
        //    {
        //        // Set any definitions left in_progress from previous run to error.
        //        SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlErrorOutInProgress, aParams);

        //        //// Look for the next rescan request.
        //        SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlTakeNextRequest, aParams);
        //        DataTable requests = SqlUtilities.GetData(_ConnectionString, _SqlNextJob);
        //        //if ((requests == null) || (requests.Rows.Count == 0)) return false;

        //        //_JobKey = (int)requests.Rows[0]["pkey"];
        //        //_JobFileName = (String)requests.Rows[0]["definition_name"];
        //        aParams[":jobkey"] = _JobKey;

        //        // Run the definition and wait till it finishes.
        //        aParams[":jobstatus"] = "Processing";
        //        aParams[":statusdescription"] = String.Format("Started: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
        //        SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlUpdateStatus, aParams);

        //        // Shell to the JAVA program and run it.
        //        RunJava();

        //        // Job finished update record.
        //        aParams[":jobstatus"] = "Completed";
        //        aParams[":statusdescription"] = String.Format("Completed: {0:MMMM d, yyyy HH:mm:ss}", DateTime.Now);
        //    }
        //    catch (Exception ex)
        //    {
        //        LocalLog.AddLine("Code Scan Error: " + ex.Message);
        //        aParams[":jobstatus"] = "Error";
        //        aParams[":statusdescription"] = String.Format("Recorded: {0:MMMM d, yyyy HH:mm:ss}, Message: {1} ", DateTime.Now, ex.Message).Substring(0, 99);

        //        // Error out this queue item and rest for this application.
        //        SqlUtilities.ExcecuteNonQuery(_ConnectionString, String.Format("UPDATE fnma_measure8.automated_queue SET ready=false, status='ERROR' WHERE ready and scan_manager_pkey={0}", _JobKey));
        //    }
        //    SqlUtilities.ExcecuteNonQuery(_ConnectionString, _SqlJobFinished, aParams);
        //    return false;
        //}

        protected void RunJava()
        {
            String args = String.Format("-s {0} -d {1} JobKey={2} DefinitionFile={3}", WrapSpaces(_SettingsFile), WrapSpaces(_JobFileName), _JobKey, _RawJobFileName);
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

            LocalLog.AddLine("Reading system tokens..");
            SetupTokens();

            LocalLog.AddLine("Reading system SQL queries..");
            String Message = "Missing the {0}.{1} SQL query. Please add the query into the settings file under Settings/Tokens/{0}.{1}";

            _SqlNextJob = ReadTokenValue("SelfServiceScan", "NextJob", Message);
            _SqlErrorOutInProgress = ReadTokenValue("SelfServiceScan", "NetErrorOldInProgress", Message);
            _SqlUpdateStatus = ReadTokenValue("SelfServiceScan", "NetUpdateStatus", Message);
            _SqlJobFinished = ReadTokenValue("SelfServiceScan", "NetFinishedProcessing", Message);
            _SqlJobStarted = ReadTokenValue("SelfServiceScan", "NetStartProcessing", Message);
            _SqlTakeNextJob = ReadTokenValue("SelfServiceScan", "NetTakeNextJob", Message);
            //_SqlNextJob = ReadTokenValue("SelfServiceScan", "NetNextJob", Message);

            LocalLog.AddLine("Reading connection information..");
            _ConnectionString = ReadAttribute("//Connections/Connection[@ID='NetScanManager']", "ConnectionString", "No ScanManager connection information found.");

            LocalLog.AddLine("Reading location of JAVA..");
            _JavaHome = ReadTokenValue("JavaHome", "Path", "Missing path to JAVA bin directory. Please add the path to Settings/Tokens/JavaHome.Path");
            if (!_JavaHome.EndsWith("\\")) _JavaHome += "\\";
            LocalLog.AddLine(String.Format("JAVA location is {0}", _JavaHome));

            LocalLog.AddLine("Reading service mode..");
            String BackupMachine = ReadOptionalTokenValue("SelfServiceScan", "DatabaseBackupServer", "False");
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

        protected void SetupTokens()
        {
            XmlNodeList nlTokenBlocks = _Settings.DocumentElement.SelectNodes("//Tokens");
            foreach (XmlNode node in nlTokenBlocks)
            {
                XmlNodeList nlTokens = node.SelectNodes("*");
                foreach (XmlNode nodeToken in nlTokens)
                {

                    XmlElement token = (XmlElement)nodeToken;
                    String tokenType = token.Name;

                    Dictionary<String, String> tokens = null;
                    if (_tokens.ContainsKey(tokenType)) { tokens = _tokens[tokenType]; }
                    else { tokens = new Dictionary<string, string>(); _tokens.Add(tokenType, tokens); }

                    foreach (XmlAttribute xA in token.Attributes)
                    {
                        String tokenKey = xA.Name;
                        String tokenValue = xA.Value;
                        if (tokens.ContainsKey(tokenKey)) { tokens[xA.Name] = tokenValue; }
                        else { tokens.Add(tokenKey, tokenValue); }
                    }
                    _tokens[tokenType] = tokens;
                }
            }
        }

        protected String ReadTokenValue(String tokenType, String tokenKey, String errorMessage)
        {
            if (!_tokens.ContainsKey(tokenType) || !_tokens[tokenType].ContainsKey(tokenKey))
                throw new Exception(String.Format(errorMessage, tokenType, tokenKey));

            return _tokens[tokenType][tokenKey];
        }

        protected String ReadOptionalTokenValue(String tokenType, String tokenKey, String defaultValue)
        {
            if (!_tokens.ContainsKey(tokenType) || !_tokens[tokenType].ContainsKey(tokenKey))
                return defaultValue;

            return _tokens[tokenType][tokenKey];
        }

        protected String ObjectToString(Object value, String errorMessage)
        {
            if ((value == null) || (value.GetType().Name.Equals("DBNull")))
                throw new Exception(errorMessage);

            return (String)value;
        }

        protected String ObjectToStringDefault(Object value, String defaultValue)
        {
            if ((value == null) || (value.GetType().Name.Equals("DBNull")))
                return defaultValue;

            return (String)value;
        }

        protected int ObjectToInteger(Object value, String errorMessage)
        {
            if ((value == null) || (value.GetType().Name.Equals("DBNull")))
                throw new Exception(errorMessage);

            return (int)value;
        }
    }
}
