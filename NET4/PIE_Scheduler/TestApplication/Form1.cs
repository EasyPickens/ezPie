using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.IO;

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
                    catch (Exception ex)
                    {
                    }
                    lblStatus.Text = "did not find the lock file: " + lockFilename;

                }
            }
        }
    }
}
