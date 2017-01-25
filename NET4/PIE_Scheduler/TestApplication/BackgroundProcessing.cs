using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;

namespace TestApplication
{
    class BackgroundProcessing
    {
        private System.ComponentModel.BackgroundWorker _bgw;

        private ScanManager.TaskManager _tm;

        public BackgroundProcessing(ScanManager.TaskManager tm)
        {
            _tm = tm;
            _bgw = new System.ComponentModel.BackgroundWorker();
            _bgw.DoWork += BackgroundWorker_DoWork;
        }

        public bool IsBusy
        {
            get { return _bgw.IsBusy; }
        }

        public bool IsAvailable
        {
            get { return !_bgw.IsBusy; }
        }

        public void DoWork()
        {
            _bgw.RunWorkerAsync();
        }

        private void BackgroundWorker_DoWork(object sender, System.ComponentModel.DoWorkEventArgs e)
        {
            _tm.ProcessQueue();
        }
    }
}
