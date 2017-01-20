using System;
using System.Diagnostics;

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
