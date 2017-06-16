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
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Linq;
using System.ServiceProcess;
using System.Text;
using System.Timers;
using System.IO;

using ScanManager.Common;
using System.Threading;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-11
 * 
 */

namespace PIE_Scheduler
{
    public partial class Service1 : ServiceBase
    {
        private System.Timers.Timer _timer = null;

        private DateTime _nextMessage = DateTime.Now.AddHours(-1);
        private DateTime _nextLogCleanUp = DateTime.Now.AddHours(-1); // causes clean-up at startup.

        private Boolean _CleanupLogs = true;

        //private ScanManager.ScanRequestManager _srm = null;
        private ScanManager.TaskManager _taskManager;
        private ScanManager.BackgroundProcessing[] _threadPool;

        public Service1()
        {
            InitializeComponent();
        }

        protected override void OnStart(string[] args)
        {
            LocalLog.AddLine("Starting the mutliprocessor scheduler service.");
            logEnvironment();
            String path = MiscUtilities.AppParentPath() + Path.DirectorySeparatorChar;
            _taskManager = new ScanManager.TaskManager();
            _CleanupLogs = _taskManager.CleanupLogs;
            _threadPool = new ScanManager.BackgroundProcessing[_taskManager.ThreadPoolSize];

            try
            {
                LocalLog.AddLine("Starting Timer: Interval is 30 seconds");
                _timer = new System.Timers.Timer(30000); // every 30 seconds
                _timer.Elapsed += new System.Timers.ElapsedEventHandler(pieJobCheck);
                _timer.Start();
                LocalLog.AddLine("Service is running.");
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Error while starting timer. " + ex.Message);
            }
        }

        //protected override void OnStart(string[] args)
        //{
        //    LocalLog.AddLine("Starting ScanManager service.");
        //    logEnvironment();
        //    String path = MiscUtilities.AppParentPath() + Path.DirectorySeparatorChar;
        //    _srm = new ScanManager.ScanRequestManager(path + "_Settings.xml", path + "pie.jar");

        //    try
        //    {
        //        LocalLog.AddLine("Starting Timer: Interval is 30 seconds");
        //        _timer = new Timer(30000); // every 30 seconds
        //        _timer.Elapsed += new System.Timers.ElapsedEventHandler(pieJobCheck);
        //        _timer.Start();
        //        LocalLog.AddLine("Service is running.");
        //    }
        //    catch (Exception ex)
        //    {
        //        LocalLog.AddLine("Error while starting timer. " + ex.Message);
        //    }
        //}

        protected override void OnStop()
        {
            if (_timer != null)
            {
                _timer.Stop();
                _timer = null;
            }
            LocalLog.AddLine("Service stopped.");
        }

        private void pieJobCheck(object sender, System.Timers.ElapsedEventArgs e)
        {
            try
            {
                _timer.Stop();
                if (DateTime.Now > _nextMessage)
                    LocalLog.AddLine("Hourly status. Still checking every 30 seconds..");

                if (_CleanupLogs && (DateTime.Now > _nextLogCleanUp))
                {
                    LocalLog.AddLine("Starting to clean up old log files ...");
                    ScanManager.Common.LogCleanUp lc = new ScanManager.Common.LogCleanUp(_taskManager.ResolveToken("PieLogs", "Path"), _taskManager.ResolveToken("PieLogs", "FileExtension"));
                    LocalLog.AddLine(lc.findOrphanFiles());
                    LocalLog.AddLine("Completed clean-up.");
                    _nextLogCleanUp = DateTime.Now.AddHours(4);
                }

                // Look for free thread
                for (int i = 0; i < _threadPool.Length; i++)
                {
                    if ((_threadPool[i] == null) || _threadPool[i].IsAvailable)
                    {
                        _threadPool[i] = new ScanManager.BackgroundProcessing(_taskManager);
                        _threadPool[i].DoWork();
                    }
                }

                if (DateTime.Now > _nextMessage)
                {
                    LocalLog.AddLine("Check complete.");
                    _nextMessage = DateTime.Now.AddHours(1);
                }
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("ERROR: " + ex.Message);
            }
            finally
            {
                String stopFile = String.Format("{0}{1}stop.txt", MiscUtilities.AppPath(), Path.DirectorySeparatorChar);
                if (File.Exists(stopFile))
                {
                    LocalLog.AddLine(String.Format("Stopping service, waiting for processes to complete. Stop file ({0}) located on the drive.", stopFile));
                    File.Delete(stopFile);

                    Boolean threadsRunning = true;
                    while (threadsRunning)
                    {
                        threadsRunning = false;
                        for (int i = 0; i < _threadPool.Length; i++)
                        {
                            if ((_threadPool[i] != null) || _threadPool[i].IsBusy)
                            {
                                threadsRunning = true;
                                break;
                            }
                        }
                        if (threadsRunning) Thread.Sleep(30000);
                    }
                    Stop();
                }
                else _timer.Start();
            }
        }

        public static void logEnvironment()
        {
            try
            {
                LocalLog.AddLine(String.Format("Process account name {0}", Environment.UserDomainName + "/" + Environment.UserName));
                LocalLog.AddLine(String.Format("Machine Name {0}", Environment.MachineName));
                LocalLog.AddLine(String.Format("Framework Version {0}", "Microsoft NET v" + Environment.Version.ToString()));
                LocalLog.AddLine(String.Format("Operating system name {0}", Environment.OSVersion));
                LocalLog.AddLine(String.Format("Is 64bit {0}", Environment.Is64BitOperatingSystem ? "Yes" : "No"));
                LocalLog.AddLine(String.Format("User working directory {0}", Environment.CurrentDirectory));
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("WARNING: Could not read all environment settings.");
                LocalLog.AddLine(String.Format("WARNING: Reason: {0}", ex.Message));
            }
        }
    }
}
