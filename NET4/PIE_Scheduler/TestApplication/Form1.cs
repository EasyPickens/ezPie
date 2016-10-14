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
    }
}
