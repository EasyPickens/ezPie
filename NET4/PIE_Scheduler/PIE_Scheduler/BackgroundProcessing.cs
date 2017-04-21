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
using System.ComponentModel;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-11
 * 
 */

namespace ScanManager
{
    class BackgroundProcessing
    {
        private BackgroundWorker _bgw;
        private TaskManager _tm;

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
