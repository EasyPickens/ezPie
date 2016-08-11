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

namespace PIE_Scheduler
{
    public partial class Service1 : ServiceBase
    {
        private Timer _timer = null;
        private DateTime _nextMessage = DateTime.Now.AddHours(-1);

        private ScanManager.ScanRequestManager _srm = null;

        public Service1()
        {
            InitializeComponent();
        }

        protected override void OnStart(string[] args)
        {
            LocalLog.AddLine("Starting ScanManager service.");
            logEnvironment();
            String path = MiscUtilities.AppParentPath() + Path.DirectorySeparatorChar;
            _srm = new ScanManager.ScanRequestManager(path + "_Settings.xml", path + "git.jar");

            try
            {
                LocalLog.AddLine("Starting Timer: Interval is 30 seconds");
                _timer = new Timer(30000); // every 30 seconds
                _timer.Elapsed += new System.Timers.ElapsedEventHandler(pieJobCheck);
                _timer.Start();
                LocalLog.AddLine("Service is running.");
            }
            catch (Exception ex)
            {
                LocalLog.AddLine("Error while starting timer. "+ex.Message);
            }
        }

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
                _srm.ProcessQueue();
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
                _timer.Start();
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
