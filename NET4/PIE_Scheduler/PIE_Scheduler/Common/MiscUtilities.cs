using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Reflection;

namespace ScanManager.Common
{
    class MiscUtilities
    {
        public static String AppPath()
        {
            return Path.GetDirectoryName(new Uri(Assembly.GetExecutingAssembly().CodeBase).LocalPath);
        }

        public static String AppParentPath()
        {
            String path = AppPath();
            int firstIndex = path.IndexOf(Path.DirectorySeparatorChar);
            int lastIndex = path.LastIndexOf(Path.DirectorySeparatorChar);
            if (firstIndex != lastIndex)
                return path.Substring(0, lastIndex);
            return path;
        }
    }
}
