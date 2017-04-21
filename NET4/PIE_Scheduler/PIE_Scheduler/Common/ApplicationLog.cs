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
using System.Diagnostics;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-11
 * 
 */

namespace ScanManager.Common
{
    class ApplicationLog
    {
        protected Boolean _canLog = true;

        String _source = "CQA Scan Manager Service";
        String _logName = "Application";

        public ApplicationLog()
        {
            //_canLog = false;
            try
            {
                if (!EventLog.SourceExists(_source))
                    EventLog.CreateEventSource(_source, _logName);
            }
            catch
            {
                _canLog = false;
            }
        }

        public void WriteEntry(String message, EventLogEntryType eventType)
        {
            if (!_canLog)
            {
                //Console.WriteLine(message);
                return;
            }

            try
            {
                using (EventLog appLog = new EventLog(_logName))
                {
                    appLog.Source = _source;
                    appLog.WriteEntry(message, eventType);
                }
            }
            catch { }
        }
    }
}
