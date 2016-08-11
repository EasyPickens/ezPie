using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading;
using System.Diagnostics;
using System.Reflection;

namespace ScanManager.Common
{
    class LocalLog
    {
        protected static String _logFilename = MiscUtilities.AppPath() + "\\ScanManagerLog.txt";

        public String LogFilename
        {
            get { return _logFilename; }
        }

        private LocalLog() { }

        public static void AddLine(String Message)
        {
            using (StreamWriter sw = new StreamWriter(_logFilename, true))
            {
                sw.WriteLine(String.Format("{0}: {1}", getMoment(), Message));
                sw.Close();
            }
        }

        protected static String getCallingClassName()
        {
            String callingClassName = "No Information";
            String callingMethod = "No Information";
            try
            {
                StackTrace stackTrace = new StackTrace();           // get call stack
                StackFrame[] stackFrames = stackTrace.GetFrames();  // get method calls (frames)

                for (int i = 0; i < stackFrames.Length; i++)
                {
                    MethodBase mb = stackFrames[i].GetMethod();
                    if (!mb.DeclaringType.Name.Equals("LogManager"))
                    {
                        callingClassName = mb.DeclaringType.Name;
                        callingMethod = mb.Name;
                        return String.Format("{0}.{1}", callingClassName, callingMethod);
                    }
                }
                return String.Format("{0}.{1}", callingClassName, callingMethod);
            }
            catch
            {
                return "Not Available";
            }
        }

        protected static String getMoment()
        {
            return DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
        }

        protected static String getThreadInfo()
        {
            return Thread.CurrentThread.ManagedThreadId.ToString();
        }
    }
}
