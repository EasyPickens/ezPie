using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.IO;
using System.Net.NetworkInformation;
using System.Threading;
using System.Threading.Tasks;

namespace TestApplication
{
    public partial class Form1 : Form
    {
        protected String _settingsFile = "";
        public Form1()
        {
            InitializeComponent();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            String pathSettings = @"C:\Developers\Code\TestDirectory\";
            String pathPie = @"C:\Developers\Code\dvts_bundle_pie\JAVA\PIE\pie\target\";
            ScanManager.ScanRequestManager srm = new ScanManager.ScanRequestManager(pathSettings + "_Settings.xml", pathPie + "pie.jar");
            srm.ProcessQueue();
            MessageBox.Show("Request Completed.", "Done", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        private void btnPathCheck_Click(object sender, EventArgs e)
        {
            String stopFile = String.Format("{0}{1}stop.txt", ScanManager.Common.MiscUtilities.AppPath(), Path.DirectorySeparatorChar);
            lblStatus.Text = stopFile;
        }

        private void btnTestNew_Click(object sender, EventArgs e)
        {
            ScanManager.TaskManager mgr = new ScanManager.TaskManager();
            mgr.ProcessQueue();
        }

        private void btnTestQuery_Click(object sender, EventArgs e)
        {

            String connString = "Server=dwsys-dbcast01;Port=2280;Database=postgres;User Id=automationtest;Password=fnmaPASS";
            String sql = "SELECT pkey, definition_name from fnma_measure8.scan_manager WHERE (in_progress and machine_name=:machinename) or true LIMIT 10";
            //String sql = "UPDATE fnma_measure8.scan_manager SET machine_name=:machinename WHERE pkey=(SELECT pkey FROM fnma_measure8.scan_manager WHERE scan_requested AND in_progress = false and machine_name is null ORDER BY request_date ASC LIMIT 1); SELECT pkey, definition_name, code_version, code_url, action_requested FROM fnma_measure8.scan_manager WHERE machine_name=:machinename and scan_requested LIMIT 1;";
            Dictionary<String, Object> sqlParams = new Dictionary<String, Object>();
            sqlParams.Add(":machinename", "bobo");

            DataTable dt = ScanManager.Common.SqlUtilities.GetData(connString, sql, sqlParams);
            lblStatus.Text = String.Format("Number of Rows returned = {0}", dt.Rows.Count);

            for (int i = 0; i < dt.Rows.Count; i++)
            {
                String definitionName = dt.Rows[i][1].ToString();
                int pos = definitionName.LastIndexOf('.');
                if (pos != -1)
                {
                    definitionName = definitionName.Substring(0, pos);
                }

                String lockFilename = String.Format("{0}{1}{2}.lck", ScanManager.Common.MiscUtilities.AppPath(), Path.DirectorySeparatorChar, definitionName);
                if (File.Exists(lockFilename))
                {
                    try
                    {
                        File.Delete(lockFilename);
                    }
                    catch { }
                    lblStatus.Text = "did not find the lock file: " + lockFilename;

                }
            }
        }

        protected int _maxParallelism = 2;
        protected int _sleepTime = 3000;
        protected Random rnd = new Random(DateTime.Now.Millisecond);

        private void btnParallel_Click(object sender, EventArgs e)
        {
            textBox1.Text = "";
            Application.DoEvents();

            //List<string> sites = new List<string> { "dwsys-apcast01", "dwsys-apcast02", "dwsys-dbcast01", "127.0.0.1", "dwsys-apcast01", "dwsys-apcast02", "dwsys-dbcast01", "127.0.0.1", "dwsys-apcast01", "dwsys-apcast02", "dwsys-dbcast01", "127.0.0.1" };
            List<string> sites = new List<string> { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7", "Item 8", "Item 9", "Item 10", "Item 11", "Item 12" };

            //List<PingReply> pingReplies = new List<PingReply>();
            List<String> pingReplies = new List<String>();
            Object mylock = new Object();
            System.Threading.Tasks.Parallel.ForEach(sites, new ParallelOptions { MaxDegreeOfParallelism = _maxParallelism }, site =>
            {
                Thread.Sleep(_sleepTime + rnd.Next(200, 1000));
                Ping p = new Ping();
                lock (mylock)
                {
                    //pingReplies.Add(p.Send(site));
                    //for (int i = 0; i < 400000000; i++)
                    //{
                    //}
                    DateTime started = DateTime.Now;
                    //PingReply pr = p.Send(site);
                    //pingReplies.Add(site + " " + pr.Address + ": " + pr.RoundtripTime + ": " + pr.Status);
                    //pingReplies.Add(String.Format("{0} {1} {2}: {3}: {4}", started.ToString("hh:mm:ss.fff"), site, pr.Address, pr.RoundtripTime, pr.Status));
                    //pingReplies.Add(String.Format("{0}: {1}", started.ToString("hh:mm:ss.fff"), site));
                    textBox1.Text += String.Format("{0}: {1} {2}", started.ToString("hh:mm:ss.fff"), site, System.Environment.NewLine);
                }
            });

            //StringBuilder sb = new StringBuilder();
            //foreach (var s in pingReplies.ToList())
            //{
            //    //Response.Write(s.Address + ": " + s.RoundtripTime + ": " + s.Status + "<br />");
            //    //sb.AppendLine(s.Address + ": " + s.RoundtripTime + ": " + s.Status);
            //    sb.AppendLine(s);
            //    textBox1.Text = sb.ToString();
            //}
        }

        private int _maxThreads = 2;
        private BackgroundProcessing[] _threadPool = new BackgroundProcessing[2];

        private ScanManager.TaskManager _taskManager = new ScanManager.TaskManager();

        private void btnBackground_Click(object sender, EventArgs e)
        {
            // Look for free thread
            for (int i = 0; i < _threadPool.Length; i++)
            {
                if ((_threadPool[i] == null) || _threadPool[i].IsAvailable)
                {
                    _threadPool[i] = new BackgroundProcessing(_taskManager);
                    _threadPool[i].DoWork();
                    return;
                }
            }
            textBox1.Text += String.Format("Maximum number of threads ({0}) are currently running.{1}", _maxThreads, System.Environment.NewLine);
            return;
        }

        private void BackgroundWorker_DoWork(object sender, System.ComponentModel.DoWorkEventArgs e)
        {
            for (int i = 0; i < 10; i++)
            {
                Thread.Sleep(1000);
            }
        }
    }
}
